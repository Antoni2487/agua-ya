// Ingreso (Figma 06 y 07): un solo formulario para el vecino (DNI + clave de 4 números) y el conductor
// (DNI + contraseña de EPSEL). El backend responde el rol y la app muestra sus pantallas.
import { useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { api, establecerSesion, ErrorApi, textoError } from '../api/cliente';
import type { AuthResponse } from '../api/tipos';
import { Isotipo } from '../componentes/iconos';
import { Aviso, Boton, Campo, Pantalla, PieServicio, Texto, tecladoNumerico } from '../componentes/ui';
import { color, fuente } from '../tema';

export function Acceso({ alRegistrarse, alOlvidarClave }: { alRegistrarse: () => void; alOlvidarClave: () => void }) {
  const [conductor, setConductor] = useState(false);
  const [dni, setDni] = useState('');
  const [clave, setClave] = useState('');
  const [errores, setErrores] = useState<{ dni?: string; clave?: string }>({});
  const [error, setError] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  const ingresar = async () => {
    setError(null);
    const e: typeof errores = {};
    if (!/^\d{8}$/.test(dni)) e.dni = 'El DNI tiene 8 dígitos, sin espacios.';
    if (conductor ? !clave : !/^\d{4}$/.test(clave)) e.clave = conductor ? 'Escribe tu contraseña.' : 'Tu clave tiene 4 números.';
    setErrores(e);
    if (e.dni || e.clave) return;
    setEnviando(true);
    try {
      const sesion = await api<AuthResponse>('/api/auth/login', { metodo: 'POST', cuerpo: { dni, password: clave }, autenticado: false });
      if (sesion.usuario.rol === 'ADMIN') { setError('Esta cuenta es de administrador: entra desde el panel web de EPSEL.'); return; }
      await establecerSesion(sesion);
    } catch (err) {
      setError(err instanceof ErrorApi && err.codigo === 'CREDENCIALES_INVALIDAS'
        ? `El DNI o la ${conductor ? 'contraseña' : 'clave'} no coinciden. Revisa e intenta de nuevo.`
        : textoError(err));
    } finally {
      setEnviando(false);
    }
  };

  const cambiarModo = () => { setConductor((c) => !c); setClave(''); setErrores({}); setError(null); };

  return (
    <Pantalla>
      <View style={estilos.marca} accessibilityRole="header" accessibilityLabel="AguaYa">
        <Isotipo tamano={60} />
        <Text style={estilos.nombre}>Agua<Text style={{ color: color.acento }}>Ya</Text></Text>
      </View>
      <View style={{ gap: 6, alignItems: 'center' }}>
        <Texto tipo="titulo" estilo={{ textAlign: 'center' }}>{conductor ? 'Ingreso del conductor' : 'Ingresa a tu cuenta'}</Texto>
        <Texto tipo="suave" estilo={{ textAlign: 'center' }}>{conductor ? 'Con tu DNI y la contraseña que te dio EPSEL.' : 'Con tu DNI y tu clave de 4 números.'}</Texto>
      </View>
      <Aviso titulo={error?.includes('no coinciden') ? 'No pudimos iniciar tu sesión' : undefined} texto={error} />
      <Campo etiqueta="DNI" valor={dni} alCambiar={(t) => setDni(t.replace(/\D/g, ''))} teclado={tecladoNumerico}
        maximo={8} ayuda="8 dígitos, sin espacios." error={errores.dni} autoCompletar="username" />
      <Campo etiqueta={conductor ? 'Contraseña' : 'Clave'} valor={clave} secreto
        alCambiar={(t) => setClave(conductor ? t : t.replace(/\D/g, ''))} teclado={conductor ? 'default' : tecladoNumerico}
        maximo={conductor ? 72 : 4} ayuda={conductor ? 'La que te entregó EPSEL.' : 'La que elegiste al crear tu cuenta.'}
        error={errores.clave} autoCompletar="password" />
      <Boton texto="Ingresar" alPresionar={ingresar} cargando={enviando} />
      {!conductor ? (
        <Pressable onPress={alOlvidarClave} accessibilityRole="link" style={estilos.enlace}>
          <Text style={estilos.enlaceTexto}>¿Olvidaste tu clave?</Text>
        </Pressable>
      ) : null}

      {!conductor ? (
        <View style={{ gap: 10, marginTop: 8 }}>
          <Texto estilo={{ textAlign: 'center' }}>¿Primera vez en AguaYa?</Texto>
          <Boton texto="Crear mi cuenta" variante="secundario" alPresionar={alRegistrarse} />
        </View>
      ) : null}
      <Pressable onPress={cambiarModo} accessibilityRole="button" style={estilos.enlace}>
        <Text style={estilos.enlaceTexto}>{conductor ? 'Soy vecino: ingresar con mi clave' : 'Soy conductor de cisterna'}</Text>
      </Pressable>
      <PieServicio />
    </Pantalla>
  );
}

const estilos = StyleSheet.create({
  marca: { alignItems: 'center', gap: 8, marginTop: 24 },
  nombre: { fontSize: 34, fontFamily: fuente.titular, color: color.tinta },
  enlace: { minHeight: 48, alignItems: 'center', justifyContent: 'center' },
  enlaceTexto: { fontSize: 17, fontFamily: fuente.negrita, color: color.acento, textDecorationLine: 'underline' },
});
