// AguaYa: una sola app para el vecino y el conductor. Según el rol de la sesión se muestran sus pantallas.
// Navegación simple por pila (sin librerías): el botón "atrás" de Android vuelve a la pantalla anterior.
import { StatusBar } from 'expo-status-bar';
import { useEffect, useState } from 'react';
import { BackHandler } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { alCambiarSesion, api, cargarSesionGuardada, establecerSesion, sesion as sesionActual } from './src/api/cliente';
import type { AuthResponse } from './src/api/tipos';
import { Cargando } from './src/componentes/ui';
import { Bienvenida } from './src/pantallas/Bienvenida';
import { InicioConductor } from './src/pantallas/conductor/InicioConductor';
import { RutaConductor } from './src/pantallas/conductor/RutaConductor';
import { LoginConductor } from './src/pantallas/LoginConductor';
import { MisReportes } from './src/pantallas/MisReportes';
import { Reportar } from './src/pantallas/Reportar';
import { Calificar } from './src/pantallas/vecino/Calificar';
import { IngresoVecino } from './src/pantallas/vecino/IngresoVecino';
import { InicioVecino } from './src/pantallas/vecino/InicioVecino';
import { RegistroVecino } from './src/pantallas/vecino/RegistroVecino';
import { darDeBajaPush, registrarPush } from './src/push';

type Destino =
  | { p: 'bienvenida' } | { p: 'loginConductor' } | { p: 'ingresoVecino' } | { p: 'registroVecino' }
  | { p: 'inicio' }
  | { p: 'ruta'; rutaId: number }
  | { p: 'reportar'; rutaId: number; paradaId?: number }
  | { p: 'calificar'; paradaId: number }
  | { p: 'reportes' };

export default function App() {
  const [listo, setListo] = useState(false);
  const [sesion, setSesion] = useState<AuthResponse | null>(null);
  const [pila, setPila] = useState<Destino[]>([{ p: 'bienvenida' }]);

  const ir = (d: Destino) => setPila((s) => [...s, d]);
  const volver = () => setPila((s) => (s.length > 1 ? s.slice(0, -1) : s));

  useEffect(() => {
    cargarSesionGuardada().then((s) => { setSesion(s); setListo(true); });
    return alCambiarSesion(setSesion);
  }, []);

  // Al entrar o salir se reinicia la pila: con sesión, el inicio de su rol; sin sesión, la bienvenida
  useEffect(() => {
    setPila([{ p: sesion ? 'inicio' : 'bienvenida' }]);
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
  const terminarYVolver = () => volver();

  let pantalla;
  if (!listo) pantalla = <Cargando texto="Abriendo AguaYa…" />;
  else if (!sesion) {
    switch (actual.p) {
      case 'loginConductor': pantalla = <LoginConductor alVolver={volver} />; break;
      case 'ingresoVecino': pantalla = <IngresoVecino alVolver={volver} alRegistrarse={() => ir({ p: 'registroVecino' })} />; break;
      case 'registroVecino': pantalla = <RegistroVecino alVolver={volver} />; break;
      default: pantalla = <Bienvenida alElegir={(r) => ir({ p: r === 'vecino' ? 'ingresoVecino' : 'loginConductor' })} />;
    }
  } else if (rol === 'CONDUCTOR') {
    switch (actual.p) {
      case 'ruta': pantalla = <RutaConductor rutaId={actual.rutaId} alVolver={volver} alReportar={(paradaId) => ir({ p: 'reportar', rutaId: actual.rutaId, paradaId })} />; break;
      case 'reportar': pantalla = <Reportar rol="CONDUCTOR" rutaId={actual.rutaId} paradaId={actual.paradaId} alVolver={volver} alTerminar={terminarYVolver} />; break;
      default: pantalla = <InicioConductor nombre={sesion.usuario.nombres.split(' ')[0]} alAbrirRuta={(rutaId) => ir({ p: 'ruta', rutaId })} alSalir={salir} />;
    }
  } else {
    switch (actual.p) {
      case 'calificar': pantalla = <Calificar paradaId={actual.paradaId} alVolver={volver} alTerminar={terminarYVolver} />; break;
      case 'reportar': pantalla = <Reportar rol="VECINO" rutaId={actual.rutaId} paradaId={actual.paradaId} alVolver={volver} alTerminar={terminarYVolver} />; break;
      case 'reportes': pantalla = <MisReportes alVolver={volver} />; break;
      default: pantalla = (
        <InicioVecino alCalificar={(paradaId) => ir({ p: 'calificar', paradaId })}
          alReportar={(rutaId, paradaId) => ir({ p: 'reportar', rutaId, paradaId })}
          alVerReportes={() => ir({ p: 'reportes' })} alSalir={salir} />
      );
    }
  }

  return (
    <SafeAreaProvider>
      <StatusBar style="light" />
      {pantalla}
    </SafeAreaProvider>
  );
}
