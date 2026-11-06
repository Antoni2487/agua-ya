// Ingreso del vecino sin contraseña: DNI → código por SMS al celular registrado → sesión.
import { useState } from 'react';
import { api, ErrorApi, establecerSesion, textoError } from '../../api/cliente';
import type { AuthResponse, SolicitarCodigoResponse } from '../../api/tipos';
import { Aviso, Boton, Campo, Pantalla, Tarjeta, Texto, tecladoNumerico } from '../../componentes/ui';

export function IngresoVecino({ alVolver, alRegistrarse }: { alVolver: () => void; alRegistrarse: () => void }) {
  const [dni, setDni] = useState('');
  const [codigo, setCodigo] = useState('');
  const [envio, setEnvio] = useState<SolicitarCodigoResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [sinCuenta, setSinCuenta] = useState(false);
  const [enviando, setEnviando] = useState(false);

  const pedirCodigo = async () => {
    setError(null); setSinCuenta(false);
    if (!/^\d{8}$/.test(dni)) { setError('El DNI tiene 8 números.'); return; }
    setEnviando(true);
    try {
      const r = await api<SolicitarCodigoResponse>('/api/auth/login/codigo/solicitar', { metodo: 'POST', cuerpo: { dni }, autenticado: false });
      setEnvio(r);
      if (r.codigo) setCodigo(r.codigo);
    } catch (e) {
      if (e instanceof ErrorApi && e.codigo === 'DNI_NO_REGISTRADO') setSinCuenta(true);
      setError(textoError(e));
    } finally {
      setEnviando(false);
    }
  };

  const ingresar = async () => {
    setError(null);
    if (!/^\d{4,10}$/.test(codigo)) { setError('Escribe el código que te llegó por SMS.'); return; }
    setEnviando(true);
    try {
      const sesion = await api<AuthResponse>('/api/auth/login/codigo', { metodo: 'POST', cuerpo: { dni, codigo }, autenticado: false });
      await establecerSesion(sesion);
    } catch (e) {
      setError(textoError(e));
    } finally {
      setEnviando(false);
    }
  };

  return (
    <Pantalla titulo="Ingreso del vecino" alVolver={alVolver}
      pie={envio
        ? <Boton texto="Entrar" alPresionar={ingresar} cargando={enviando} />
        : <Boton texto="Enviarme el código por SMS" alPresionar={pedirCodigo} cargando={enviando} />}>
      {!envio ? (
        <>
          <Texto tipo="suave">No necesitas contraseña: te mandamos un código al celular con el que te registraste.</Texto>
          <Campo etiqueta="Tu DNI" valor={dni} alCambiar={(t) => setDni(t.replace(/\D/g, ''))} teclado={tecladoNumerico} maximo={8} autoFocus />
        </>
      ) : (
        <>
          <Tarjeta tono="claro">
            <Texto>Te enviamos un código al celular {envio.telefonoEnmascarado ?? 'registrado'}.</Texto>
            {envio.codigo ? <Texto tipo="pequeno">Modo demostración: el código ya está escrito abajo.</Texto> : null}
          </Tarjeta>
          <Campo etiqueta="Código del SMS" valor={codigo} alCambiar={(t) => setCodigo(t.replace(/\D/g, ''))}
            teclado={tecladoNumerico} maximo={10} autoFocus />
          <Boton texto="Cambiar DNI o volver a enviar" variante="secundario" alPresionar={() => { setEnvio(null); setCodigo(''); }} />
        </>
      )}
      <Aviso texto={error} />
      {sinCuenta ? <Boton texto="Crear mi cuenta" variante="claro" alPresionar={alRegistrarse} /> : null}
      {!envio && !sinCuenta ? (
        <Boton texto="¿Primera vez? Regístrate" variante="secundario" alPresionar={alRegistrarse} />
      ) : null}
    </Pantalla>
  );
}
