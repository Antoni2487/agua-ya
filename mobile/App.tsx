// AguaYa: una sola app para el vecino y el conductor. Según el rol de la sesión se muestran sus pantallas.
// Navegación simple por pila (sin librerías): el botón "atrás" de Android vuelve a la pantalla anterior.
import { AtkinsonHyperlegible_400Regular, AtkinsonHyperlegible_700Bold } from '@expo-google-fonts/atkinson-hyperlegible';
import { Nunito_700Bold, Nunito_800ExtraBold, useFonts } from '@expo-google-fonts/nunito';
import { StatusBar } from 'expo-status-bar';
import { useEffect, useState } from 'react';
import { BackHandler } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { alCambiarSesion, api, cargarSesionGuardada, establecerSesion, sesion as sesionActual } from './src/api/cliente';
import type { AuthResponse } from './src/api/tipos';
import { Cargando } from './src/componentes/ui';
import { Acceso } from './src/pantallas/Acceso';
import { InicioConductor } from './src/pantallas/conductor/InicioConductor';
import { RutaConductor } from './src/pantallas/conductor/RutaConductor';
import { Reportar } from './src/pantallas/Reportar';
import { RegistroVecino } from './src/pantallas/vecino/RegistroVecino';
import { RestablecerClave } from './src/pantallas/vecino/RestablecerClave';
import { VecinoApp } from './src/pantallas/vecino/VecinoApp';
import { darDeBajaPush, registrarPush } from './src/push';

type Destino =
  | { p: 'acceso' } | { p: 'registro' } | { p: 'restablecer' }
  | { p: 'inicio' }
  | { p: 'ruta'; rutaId: number }
  | { p: 'reportar'; rutaId: number; paradaId?: number };

export default function App() {
  const [fuentes] = useFonts({ Nunito_700Bold, Nunito_800ExtraBold, AtkinsonHyperlegible_400Regular, AtkinsonHyperlegible_700Bold });
  const [listo, setListo] = useState(false);
  const [sesion, setSesion] = useState<AuthResponse | null>(null);
  const [pila, setPila] = useState<Destino[]>([{ p: 'acceso' }]);

  const ir = (d: Destino) => setPila((s) => [...s, d]);
  const volver = () => setPila((s) => (s.length > 1 ? s.slice(0, -1) : s));

  useEffect(() => {
    cargarSesionGuardada().then((s) => { setSesion(s); setListo(true); });
    return alCambiarSesion(setSesion);
  }, []);

  // Al entrar o salir se reinicia la pila: con sesión, el inicio de su rol; sin sesión, el ingreso
  useEffect(() => {
    setPila([{ p: sesion ? 'inicio' : 'acceso' }]);
    if (sesion && sesion.usuario.rol !== 'ADMIN') void registrarPush();
  }, [sesion?.usuario.id]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    const sub = BackHandler.addEventListener('hardwareBackPress', () => {
      if (pila.length > 1) { volver(); return true; }
      return false;
    });
    return () => sub.remove();
  }, [pila.length]);

  const salir = async () => {
    await darDeBajaPush();
    const refreshToken = sesionActual()?.refreshToken;
    if (refreshToken) await api('/api/auth/logout', { metodo: 'POST', cuerpo: { refreshToken }, autenticado: false }).catch(() => undefined);
    await establecerSesion(null);
  };

  const actual = pila[pila.length - 1];
  const rol = sesion?.usuario.rol;

  let pantalla;
  if (!listo || !fuentes) pantalla = <Cargando texto="Abriendo AguaYa…" />;
  else if (!sesion) {
    switch (actual.p) {
      case 'registro': pantalla = <RegistroVecino alVolver={volver} />; break;
      case 'restablecer': pantalla = <RestablecerClave alVolver={volver} />; break;
      default: pantalla = <Acceso alRegistrarse={() => ir({ p: 'registro' })} alOlvidarClave={() => ir({ p: 'restablecer' })} />;
    }
  } else if (rol === 'CONDUCTOR') {
    switch (actual.p) {
      case 'ruta': pantalla = <RutaConductor rutaId={actual.rutaId} alVolver={volver} alReportar={(paradaId) => ir({ p: 'reportar', rutaId: actual.rutaId, paradaId })} />; break;
      case 'reportar': pantalla = <Reportar rol="CONDUCTOR" rutaId={actual.rutaId} paradaId={actual.paradaId} alVolver={volver} alTerminar={volver} />; break;
      default: pantalla = <InicioConductor nombre={sesion.usuario.nombres.split(' ')[0]} alAbrirRuta={(rutaId) => ir({ p: 'ruta', rutaId })} alSalir={salir} />;
    }
  } else {
    pantalla = <VecinoApp alSalir={salir} />;
  }

  return (
    <SafeAreaProvider>
      <StatusBar style="dark" />
      {pantalla}
    </SafeAreaProvider>
  );
}
