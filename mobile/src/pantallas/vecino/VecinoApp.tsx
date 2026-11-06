// La app del vecino: cuatro pestañas (Inicio, Mapa, Reportar, Mi cuenta) como en el Figma, con los datos de su
// atención cargados una sola vez y compartidos. Calificar y Mis reportes se abren encima de las pestañas.
import { useCallback, useEffect, useState } from 'react';
import { BackHandler } from 'react-native';
import { api, textoError } from '../../api/cliente';
import type { Atencion, AtencionVecino, VecinoPerfil } from '../../api/tipos';
import { BarraVecino, type PestanaVecino } from '../../componentes/vecino';
import { MisReportes } from '../MisReportes';
import { Reportar } from '../Reportar';
import { Calificar } from './Calificar';
import { CuentaVecino } from './CuentaVecino';
import { InicioVecino } from './InicioVecino';
import { MapaVecino } from './MapaVecino';

export interface DatosVecino {
  perfil: VecinoPerfil | null;
  atencion: AtencionVecino | null;
  error: string | null;
  cargando: boolean;
  recargar: () => Promise<void>;
}

type Encima = { p: 'calificar'; atencion: Atencion } | { p: 'reportes' } | null;

export function VecinoApp({ alSalir }: { alSalir: () => void }) {
  const [pestana, setPestana] = useState<PestanaVecino>('inicio');
  const [encima, setEncima] = useState<Encima>(null);
  const [perfil, setPerfil] = useState<VecinoPerfil | null>(null);
  const [atencion, setAtencion] = useState<AtencionVecino | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [cargando, setCargando] = useState(true);

  const recargar = useCallback(async () => {
    setError(null);
    try {
      const [p, a] = await Promise.all([
        api<VecinoPerfil>('/api/vecino/me'),
        api<AtencionVecino>('/api/vecino/proxima-atencion'),
      ]);
      setPerfil(p); setAtencion(a);
    } catch (e) {
      setError(textoError(e));
    } finally {
      setCargando(false);
    }
  }, []);

  useEffect(() => {
    void recargar();
    const t = setInterval(() => void recargar(), 30000); // el avance de la ruta cambia cada pocos minutos
    return () => clearInterval(t);
  }, [recargar]);

  // Atrás de Android: cierra lo que está encima o vuelve a Inicio
  useEffect(() => {
    const sub = BackHandler.addEventListener('hardwareBackPress', () => {
      if (encima) { setEncima(null); return true; }
      if (pestana !== 'inicio') { setPestana('inicio'); return true; }
      return false;
    });
    return () => sub.remove();
  }, [encima, pestana]);

  const datos: DatosVecino = { perfil, atencion, error, cargando, recargar };
  const cerrarYRecargar = () => { setEncima(null); void recargar(); };

  if (encima?.p === 'calificar') {
    return <Calificar atencion={encima.atencion} alVolver={() => setEncima(null)} alTerminar={cerrarYRecargar} />;
  }
  if (encima?.p === 'reportes') return <MisReportes alVolver={() => setEncima(null)} />;

  const barra = <BarraVecino activa={pestana} alElegir={setPestana} />;
  const rutaParaReportar = atencion?.proxima ?? atencion?.ultima ?? null;

  switch (pestana) {
    case 'mapa':
      return <MapaVecino datos={datos} barra={barra} alVolver={() => setPestana('inicio')} />;
    case 'reportar':
      return (
        <Reportar rol="VECINO" atencion={rutaParaReportar} sector={perfil?.sector?.nombre}
          barra={barra} alVolver={() => setPestana('inicio')} alVerReportes={() => setEncima({ p: 'reportes' })} />
      );
    case 'cuenta':
      return <CuentaVecino perfil={perfil} barra={barra} alVerReportes={() => setEncima({ p: 'reportes' })} alSalir={alSalir} />;
    default:
      return (
        <InicioVecino datos={datos} barra={barra}
          alVerMapa={() => setPestana('mapa')}
          alReportar={() => setPestana('reportar')}
          alCalificar={(a) => setEncima({ p: 'calificar', atencion: a })} />
      );
  }
}
