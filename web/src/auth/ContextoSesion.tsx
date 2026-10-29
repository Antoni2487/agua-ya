import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';

import {
  alCambiarSesion, api, ErrorApi, establecerSesion, hayRefreshGuardado, refrescarSesion, refreshGuardado,
} from '../api/cliente';
import type { AuthResponse, PersonaResumen } from '../api/tipos';

interface Sesion {
  usuario: PersonaResumen | null;
  /** true mientras se intenta recuperar la sesión guardada al abrir la página. */
  cargando: boolean;
  iniciarSesion: (dni: string, password: string) => Promise<void>;
  cerrarSesion: () => Promise<void>;
}

const ContextoSesion = createContext<Sesion | null>(null);

/** El panel web es solo para el Administrador de EPSEL; vecinos y conductores usan la app móvil. */
export function ProveedorSesion({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<PersonaResumen | null>(null);
  const [cargando, setCargando] = useState(hayRefreshGuardado());

  useEffect(() => alCambiarSesion((sesion) => setUsuario(sesion?.usuario ?? null)), []);

  useEffect(() => {
    if (!hayRefreshGuardado()) return;
    refrescarSesion().finally(() => setCargando(false));
  }, []);

  const iniciarSesion = useCallback(async (dni: string, password: string) => {
    const sesion = await api<AuthResponse>('/api/auth/login', {
      metodo: 'POST',
      cuerpo: { dni, password },
      autenticado: false,
    });
    if (sesion.usuario.rol !== 'ADMIN') {
      await api('/api/auth/logout', { metodo: 'POST', cuerpo: { refreshToken: sesion.refreshToken }, autenticado: false })
        .catch(() => undefined);
      throw new ErrorApi(403, 'SOLO_ADMIN', 'El panel web es solo para administradores. Vecinos y conductores usan la app móvil.');
    }
    establecerSesion(sesion);
  }, []);

  const cerrarSesion = useCallback(async () => {
    const refreshToken = refreshGuardado();
    if (refreshToken) {
      await api('/api/auth/logout', { metodo: 'POST', cuerpo: { refreshToken }, autenticado: false }).catch(() => undefined);
    }
    establecerSesion(null);
  }, []);

  const valor = useMemo(() => ({ usuario, cargando, iniciarSesion, cerrarSesion }),
    [usuario, cargando, iniciarSesion, cerrarSesion]);
  return <ContextoSesion.Provider value={valor}>{children}</ContextoSesion.Provider>;
}

export function useSesion(): Sesion {
  const sesion = useContext(ContextoSesion);
  if (!sesion) throw new Error('useSesion debe usarse dentro de <ProveedorSesion>');
  return sesion;
}
