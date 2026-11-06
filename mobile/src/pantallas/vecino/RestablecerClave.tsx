// ¿Olvidaste tu clave?: el código SMS llega al celular registrado y con él el vecino elige una clave nueva.
import { useState } from 'react';
import { api, establecerSesion, textoError } from '../../api/cliente';
import type { AuthResponse, SolicitarCodigoResponse } from '../../api/tipos';
import { Aviso, Boton, Campo, Pantalla, Tarjeta, Texto, tecladoNumerico } from '../../componentes/ui';

export function RestablecerClave({ alVolver }: { alVolver: () => void }) {
  const [dni, setDni] = useState('');
  const [envio, setEnvio] = useState<SolicitarCodigoResponse | null>(null);
  const [codigo, setCodigo] = useState('');
  const [clave, setClave] = useState('');
  const [repetir, setRepetir] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  const pedirCodigo = async () => {
    setError(null);
    if (!/^\d{8}$/.test(dni)) { setError('El DNI tiene 8 dígitos.'); return; }
    setEnviando(true);
    try {
      const r = await api<SolicitarCodigoResponse>('/api/auth/login/codigo/solicitar', { metodo: 'POST', cuerpo: { dni }, autenticado: false });
      setEnvio(r);
      if (r.codigo) setCodigo(r.codigo);
    } catch (e) {
      setError(textoError(e));
    } finally {
      setEnviando(false);
    }
  };

  const guardar = async () => {
    setError(null);
    if (!/^\d{4,10}$/.test(codigo)) { setError('Escribe el código que te llegó por SMS.'); return; }
    if (!/^\d{4}$/.test(clave)) { setError('La clave tiene 4 números.'); return; }
    if (clave !== repetir) { setError('Las dos claves no son iguales.'); return; }
    setEnviando(true);
    try {
      const sesion = await api<AuthResponse>('/api/auth/clave/restablecer', { metodo: 'POST', cuerpo: { dni, codigo, clave }, autenticado: false });
      await establecerSesion(sesion);
    } catch (e) {
      setError(textoError(e));
    } finally {
      setEnviando(false);
    }
  };

  return (
    <Pantalla titulo="Cambiar mi clave" alVolver={alVolver}>
      {!envio ? (
        <>
          <Texto>Te enviaremos un código por SMS al celular con el que te registraste.</Texto>
          <Campo etiqueta="Tu DNI" valor={dni} alCambiar={(t) => setDni(t.replace(/\D/g, ''))} teclado={tecladoNumerico} maximo={8} autoFocus />
          <Aviso texto={error} />
          <Boton texto="Enviarme el código" alPresionar={pedirCodigo} cargando={enviando} />
        </>
      ) : (
        <>
          <Tarjeta tono="claro">
            <Texto>Te enviamos un código al celular {envio.telefonoEnmascarado ?? ''}.</Texto>
            {envio.codigo ? <Texto tipo="pequeno">Modo demostración: el código ya está escrito abajo.</Texto> : null}
          </Tarjeta>
          <Campo etiqueta="Código del SMS" valor={codigo} alCambiar={(t) => setCodigo(t.replace(/\D/g, ''))} teclado={tecladoNumerico} maximo={10} autoCompletar="one-time-code" />
          <Campo etiqueta="Nueva clave" ayuda="4 números que recuerdes. No uses tu fecha de nacimiento." valor={clave}
            alCambiar={(t) => setClave(t.replace(/\D/g, ''))} teclado={tecladoNumerico} maximo={4} secreto />
          <Campo etiqueta="Repite la clave" valor={repetir} alCambiar={(t) => setRepetir(t.replace(/\D/g, ''))} teclado={tecladoNumerico} maximo={4} secreto />
          <Aviso texto={error} />
          <Boton texto="Guardar e ingresar" alPresionar={guardar} cargando={enviando} />
        </>
      )}
    </Pantalla>
  );
}
