import { useState } from 'react';
import { api, establecerSesion, textoError } from '../api/cliente';
import type { AuthResponse } from '../api/tipos';
import { Aviso, Boton, Campo, Pantalla, Texto, tecladoNumerico } from '../componentes/ui';

export function LoginConductor({ alVolver }: { alVolver: () => void }) {
  const [dni, setDni] = useState('');
  const [clave, setClave] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  const ingresar = async () => {
    setError(null);
    if (!/^\d{8}$/.test(dni)) { setError('El DNI tiene 8 números.'); return; }
    if (!clave) { setError('Escribe tu contraseña.'); return; }
    setEnviando(true);
    try {
      const sesion = await api<AuthResponse>('/api/auth/login', { metodo: 'POST', cuerpo: { dni, password: clave }, autenticado: false });
      if (sesion.usuario.rol !== 'CONDUCTOR') {
        setError(sesion.usuario.rol === 'ADMIN'
          ? 'Esta cuenta es de administrador: entra desde el panel web.'
          : 'Esta cuenta es de vecino: vuelve y elige «Soy vecino».');
        return;
      }
      await establecerSesion(sesion);
    } catch (e) {
      setError(textoError(e));
    } finally {
      setEnviando(false);
    }
  };

  return (
    <Pantalla titulo="Ingreso del conductor" alVolver={alVolver}
      pie={<Boton texto="Ingresar" alPresionar={ingresar} cargando={enviando} />}>
      <Texto tipo="suave">Usa el DNI y la contraseña que te dio EPSEL.</Texto>
      <Campo etiqueta="DNI" valor={dni} alCambiar={(t) => setDni(t.replace(/\D/g, ''))} teclado={tecladoNumerico} maximo={8} autoFocus />
      <Campo etiqueta="Contraseña" valor={clave} alCambiar={setClave} secreto maximo={72} />
      <Aviso texto={error} />
    </Pantalla>
  );
}
