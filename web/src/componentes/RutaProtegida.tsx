import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';

import { useSesion } from '../auth/ContextoSesion';
import { Cargando } from './Estado';

/** Solo el Administrador entra al panel; si no hay sesión, al login (y luego vuelve a donde iba). */
export function RutaProtegida({ children }: { children: ReactNode }) {
  const { usuario, cargando } = useSesion();
  const ubicacion = useLocation();
  if (cargando) return <Cargando texto="Recuperando tu sesión…" />;
  if (!usuario || usuario.rol !== 'ADMIN') {
    return <Navigate to="/login" replace state={{ desde: ubicacion.pathname }} />;
  }
  return <>{children}</>;
}
