import { useQueries, useQuery, useQueryClient } from '@tanstack/react-query';
import { lazy, Suspense, useCallback, useState } from 'react';
import { Link } from 'react-router-dom';

import { api } from '../../api/cliente';
import { useEstadoTiempoReal, useSuscripcion } from '../../api/tiempoReal';
import type { EventoRuta, Pagina, Posicion, Recorrido, RutaResumen } from '../../api/tipos';
import { Cargando, MensajeError } from '../../componentes/Estado';
import type { RutaEnMapa } from '../../componentes/MapaEnVivo';
import { useAhora } from '../../hooks';
import { EVENTOS_RUTA, hace, hora, hoyEnLima } from '../../utilidades';

const MapaEnVivo = lazy(() => import('../../componentes/MapaEnVivo').then((m) => ({ default: m.MapaEnVivo })));

/** El conductor envía GPS cada ~15 s; sin posición en 2 min (vigencia de la geocerca) se marca "sin señal". */
const SIN_SENAL_MS = 120_000;
const MAX_EVENTOS = 30;

/** RF04: cisternas en ruta en tiempo real, con su avance y los eventos del día. */
export function Monitoreo() {
  const hoy = hoyEnLima();
  const ahora = useAhora();
  const conexion = useEstadoTiempoReal();
  const clienteConsultas = useQueryClient();
  const [enVivo, setEnVivo] = useState<Record<number, Posicion[]>>({});
  const [eventos, setEventos] = useState<EventoRuta[]>([]);
  const [seleccionada, setSeleccionada] = useState<number | null>(null);

  const rutas = useQuery({
    queryKey: ['rutas', { fecha: hoy, estado: 'EN_CURSO' }],
    queryFn: ({ signal }) =>
      api<Pagina<RutaResumen>>('/api/admin/rutas', { parametros: { fecha: hoy, estado: 'EN_CURSO', size: 100 }, signal }),
  });
  const enCurso = rutas.data?.contenido ?? [];

  const recorridos = useQueries({
    queries: enCurso.map((r) => ({
      queryKey: ['recorrido', r.id],
      queryFn: ({ signal }: { signal: AbortSignal }) => api<Recorrido>(`/api/admin/rutas/${r.id}/recorrido`, { signal }),
      // Al volver a la pantalla se recarga: las posiciones recibidas mientras no estaba abierta no se guardan.
      refetchOnMount: 'always' as const,
    })),
  });

  const agregarPosicion = useCallback((p: Posicion) => {
    setEnVivo((actual) => ({ ...actual, [p.rutaId]: [...(actual[p.rutaId] ?? []), p] }));
  }, []);

  useSuscripcion<EventoRuta>('/topic/admin/rutas', (evento) => {
    setEventos((actuales) => [evento, ...actuales].slice(0, MAX_EVENTOS));
    void clienteConsultas.invalidateQueries({ queryKey: ['rutas'] });
    void clienteConsultas.invalidateQueries({ queryKey: ['ruta', evento.rutaId] });
  });

  const enMapa: RutaEnMapa[] = enCurso.map((ruta, i) => {
    const historicas = recorridos[i]?.data?.posiciones ?? [];
    const ultimaHistorica = historicas.at(-1)?.registradoEn ?? '';
    const nuevas = (enVivo[ruta.id] ?? []).filter((p) => p.registradoEn > ultimaHistorica);
    const posiciones = [...historicas, ...nuevas];
    const ultima = posiciones.at(-1);
    return { ruta, posiciones, sinSenal: !ultima || ahora - new Date(ultima.registradoEn).getTime() > SIN_SENAL_MS };
  });
  const nombreRuta = (rutaId: number) => enCurso.find((r) => r.id === rutaId)?.sectorNombre ?? `Ruta ${rutaId}`;

  return (
    <section>
      <div className="encabezado">
        <h1>Mapa en vivo</h1>
        {conexion !== 'conectado' && (
          <span className="alerta alerta-aviso">Sin conexión en vivo: los datos pueden estar desactualizados.</span>
        )}
      </div>
      {enCurso.map((r) => (
        <SuscripcionPosicion key={r.id} rutaId={r.id} alRecibir={agregarPosicion} />
      ))}

      {rutas.isPending && <Cargando />}
      {rutas.isError && <MensajeError error={rutas.error} reintentar={() => void rutas.refetch()} />}
      {rutas.data && (
        <div className="monitoreo">
          <Suspense fallback={<div className="mapa mapa-cargando"><Cargando texto="Cargando mapa…" /></div>}>
            <MapaEnVivo rutas={enMapa} seleccionada={seleccionada} alSeleccionar={setSeleccionada} />
          </Suspense>
          <div className="lista-en-vivo">
            {enCurso.length === 0 && (
              <p className="texto-suave">
                No hay cisternas en ruta ahora. Cuando un conductor inicie su ruta aparecerá aquí sin recargar.
              </p>
            )}
            {enMapa.map(({ ruta, posiciones, sinSenal }) => {
              const ultima = posiciones.at(-1);
              const avance = ruta.totalParadas ? (ruta.paradasAtendidas / ruta.totalParadas) * 100 : 0;
              return (
                <article
                  key={ruta.id}
                  className={`tarjeta ruta-en-vivo${seleccionada === ruta.id ? ' seleccionada' : ''}`}
                  onClick={() => setSeleccionada(ruta.id)}
                >
                  <header>
                    <strong>{ruta.sectorNombre}</strong>
                    <span className="placa">{ruta.cisternaPlaca}</span>
                  </header>
                  <span className="texto-suave">{ruta.conductorNombre}</span>
                  <div className="barra-avance" aria-label={`Avance ${Math.round(avance)} %`}>
                    <span style={{ width: `${avance}%` }} />
                  </div>
                  <span>
                    {ruta.paradasAtendidas}/{ruta.totalParadas} paradas atendidas
                  </span>
                  <span className={sinSenal ? 'sin-senal' : 'texto-suave'}>
                    {ultima ? `${sinSenal ? 'Sin señal · ' : ''}última posición ${hace(ultima.registradoEn, ahora)}` : 'Aún sin posición GPS'}
                  </span>
                  <Link to={`/admin/rutas/${ruta.id}`} onClick={(e) => e.stopPropagation()}>
                    Ver detalle
                  </Link>
                </article>
              );
            })}

            <h2>Eventos</h2>
            {eventos.length === 0 && <p className="texto-suave">Los eventos de las rutas aparecerán aquí en tiempo real.</p>}
            <ul className="eventos" aria-live="polite">
              {eventos.map((e) => (
                <li key={`${e.rutaId}-${e.tipo}-${e.registradoEn}-${e.paradaId ?? ''}`}>
                  <time>{hora(e.registradoEn)}</time>
                  <span>
                    <Link to={`/admin/rutas/${e.rutaId}`}>{nombreRuta(e.rutaId)}</Link> {EVENTOS_RUTA[e.tipo]}
                  </span>
                </li>
              ))}
            </ul>
          </div>
        </div>
      )}
    </section>
  );
}

function SuscripcionPosicion({ rutaId, alRecibir }: { rutaId: number; alRecibir: (p: Posicion) => void }) {
  useSuscripcion<Posicion>(`/topic/rutas/${rutaId}/posicion`, alRecibir);
  return null;
}
