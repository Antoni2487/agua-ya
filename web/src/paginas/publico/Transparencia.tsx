import { useQuery } from '@tanstack/react-query';
import { lazy, Suspense, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';

import { api } from '../../api/cliente';
import type { CronogramaItem, ResumenTransparencia, SectorTransparencia } from '../../api/tipos';
import { Cargando, MensajeError } from '../../componentes/Estado';
import {
  contar, diaSemana, ESTADOS_RUTA, fechaCorta, hoyEnLima, numero, porcentaje, sumarDias, TURNOS, volumen,
} from '../../utilidades';

// Leaflet pesa ~150 KB: se carga aparte para que los indicadores aparezcan primero.
const MapaSectores = lazy(() => import('../../componentes/MapaSectores').then((m) => ({ default: m.MapaSectores })));

const PERIODOS = [
  { dias: 7, etiqueta: 'Últimos 7 días' },
  { dias: 30, etiqueta: 'Últimos 30 días' },
  { dias: 90, etiqueta: 'Últimos 90 días' },
];

/**
 * Panel público de transparencia (RF09): sin login y solo con datos agregados. Nunca muestra nombres,
 * teléfonos ni ubicaciones de viviendas (regla 9); del mapa solo salen los centros de sector.
 */
export function Transparencia() {
  const hoy = hoyEnLima();
  const [dias, setDias] = useState(30);
  const [distrito, setDistrito] = useState('');
  const desde = sumarDias(hoy, -(dias - 1));

  const resumen = useQuery({
    queryKey: ['transparencia', 'resumen', desde, hoy],
    queryFn: ({ signal }) =>
      api<ResumenTransparencia>('/api/public/transparencia/resumen', { parametros: { desde, hasta: hoy }, autenticado: false, signal }),
  });
  const sectores = useQuery({
    queryKey: ['transparencia', 'sectores', desde, hoy],
    queryFn: ({ signal }) =>
      api<SectorTransparencia[]>('/api/public/transparencia/sectores', { parametros: { desde, hasta: hoy }, autenticado: false, signal }),
  });
  const cronograma = useQuery({
    queryKey: ['transparencia', 'cronograma', hoy],
    queryFn: ({ signal }) =>
      api<CronogramaItem[]>('/api/public/transparencia/cronograma', { parametros: { desde: hoy }, autenticado: false, signal }),
  });

  const distritos = useMemo(
    () => [...new Set((sectores.data ?? []).map((s) => s.distrito))].sort((a, b) => a.localeCompare(b, 'es')),
    [sectores.data],
  );
  const sectoresVisibles = (sectores.data ?? []).filter((s) => !distrito || s.distrito === distrito);
  const cronogramaVisible = (cronograma.data ?? []).filter((r) => !distrito || r.distrito === distrito);
  const porDia = agruparPorFecha(cronogramaVisible);

  return (
    <div className="publico">
      <header className="barra">
        <span className="marca">
          <img src="/gota.svg" alt="" width={22} height={22} />
          AguaYa <small>Transparencia</small>
        </span>
        <Link to="/login" className="enlace-barra">
          Ingreso EPSEL
        </Link>
      </header>

      <main className="contenido">
        <h1>Abastecimiento de agua con cisternas</h1>
        <p className="texto-suave intro">
          Cuándo llega la cisterna a cada sector y cómo se viene cumpliendo el servicio en Chiclayo y distritos aledaños.
        </p>

        <div className="filtros">
          <label>
            Distrito
            <select value={distrito} onChange={(e) => setDistrito(e.target.value)}>
              <option value="">Todos</option>
              {distritos.map((d) => (
                <option key={d}>{d}</option>
              ))}
            </select>
          </label>
          <label>
            Periodo de los indicadores
            <select value={dias} onChange={(e) => setDias(Number(e.target.value))}>
              {PERIODOS.map((p) => (
                <option key={p.dias} value={p.dias}>
                  {p.etiqueta}
                </option>
              ))}
            </select>
          </label>
        </div>

        <h2>Próximas atenciones</h2>
        {cronograma.isPending && <Cargando />}
        {cronograma.isError && <MensajeError error={cronograma.error} reintentar={() => void cronograma.refetch()} />}
        {cronograma.data && porDia.length === 0 && (
          <p className="texto-suave">No hay rutas programadas para los próximos 7 días{distrito && ` en ${distrito}`}.</p>
        )}
        <div className="cronograma">
          {porDia.map(([fecha, rutas]) => (
            <section key={fecha} className="tarjeta dia" aria-label={diaSemana(fecha)}>
              <h3>
                {fecha === hoy ? 'Hoy' : diaSemana(fecha)}
                {fecha === hoy && <small className="texto-suave"> · {fechaCorta(fecha)}</small>}
              </h3>
              <ul>
                {rutas.map((r) => (
                  <li key={r.rutaId}>
                    <div>
                      <strong>{r.sectorNombre}</strong>
                      <span className="texto-suave">
                        {' '}
                        · {r.distrito} · {TURNOS[r.turno]}
                      </span>
                    </div>
                    <div className="dia-detalle">
                      <span className={`etiqueta etiqueta-${r.estado.toLowerCase()}`}>{ESTADOS_RUTA[r.estado]}</span>
                      {r.cisternaPlaca && <span className="placa" title="Placa de la cisterna asignada">{r.cisternaPlaca}</span>}
                      {r.estado !== 'PROGRAMADA' && r.totalParadas > 0 && (
                        <span className="texto-suave">
                          {r.paradasAtendidas}/{r.totalParadas} paradas
                        </span>
                      )}
                    </div>
                  </li>
                ))}
              </ul>
            </section>
          ))}
        </div>

        <h2>Sectores</h2>
        {sectores.isPending && <Cargando />}
        {sectores.isError && <MensajeError error={sectores.error} reintentar={() => void sectores.refetch()} />}
        {sectores.data && (
          <div className="sectores">
            <Suspense fallback={<div className="mapa mapa-cargando"><Cargando texto="Cargando mapa…" /></div>}>
              <MapaSectores sectores={sectoresVisibles} />
            </Suspense>
            <ul className="lista-sectores">
              {sectoresVisibles.map((s) => (
                <li key={s.sectorId} className="tarjeta">
                  <div className="sector-titulo">
                    <strong>{s.nombre}</strong>
                    <span className="texto-suave">
                      {s.distrito}
                      {s.esZonaAlta && ' · zona alta'}
                      {s.tipoEntrega === 'PUNTO_ENTREGA' ? ' · puntos de entrega' : ' · a domicilio'}
                    </span>
                  </div>
                  <dl>
                    <dt>Próxima atención</dt>
                    <dd>
                      {s.proximaAtencion
                        ? `${fechaCorta(s.proximaAtencion.fecha)} · ${TURNOS[s.proximaAtencion.turno]}`
                        : 'Sin programar'}
                    </dd>
                    <dt>Última atención</dt>
                    <dd>{s.ultimaAtencion ? fechaCorta(s.ultimaAtencion) : '—'}</dd>
                    <dt>Cumplimiento</dt>
                    <dd>{porcentaje(s.cumplimientoPorcentaje)}</dd>
                    <dt>Calificación</dt>
                    <dd>{s.calificacionPromedio === null ? '—' : `${s.calificacionPromedio.toFixed(1)} ★`}</dd>
                  </dl>
                </li>
              ))}
            </ul>
          </div>
        )}

        <h2>
          Indicadores · {fechaCorta(desde)} al {fechaCorta(hoy)}
          <small className="texto-suave"> (todos los distritos)</small>
        </h2>
        {resumen.isPending && <Cargando />}
        {resumen.isError && <MensajeError error={resumen.error} reintentar={() => void resumen.refetch()} />}
        {resumen.data && <Indicadores r={resumen.data} />}

        <footer className="pie texto-suave">
          Datos referenciales del proyecto académico AguaYa (UTP Chiclayo), no oficiales de EPSEL. Se muestran solo datos
          agregados: nunca nombres, teléfonos ni ubicaciones de viviendas. El cumplimiento considera las paradas de rutas ya
          cerradas; las reasignadas cuentan en la ruta que las atendió.
        </footer>
      </main>
    </div>
  );
}

function Indicadores({ r }: { r: ResumenTransparencia }) {
  const cumplimiento = r.paradas.cumplimientoPorcentaje;
  return (
    <div className="indicadores">
      <Dato
        titulo="Cumplimiento de entregas"
        valor={porcentaje(cumplimiento)}
        detalle={`${numero(r.paradas.atendidas)} de ${contar(r.paradas.total, 'parada atendida', 'paradas atendidas')}`}
        tono={cumplimiento === null ? undefined : cumplimiento >= 90 ? 'bueno' : cumplimiento < 70 ? 'malo' : undefined}
      />
      <Dato titulo="Agua distribuida" valor={volumen(r.litrosDistribuidos)} detalle={contar(r.rutas.completadas, 'ruta completada', 'rutas completadas')} />
      <Dato
        titulo="Rutas interrumpidas"
        valor={numero(r.rutas.interrumpidas)}
        detalle={`de ${contar(r.rutas.total, 'ruta', 'rutas')} del periodo`}
      />
      <Dato
        titulo="Calificación de vecinos"
        valor={r.calificacion.promedio === null ? '—' : `${r.calificacion.promedio.toFixed(1)} ★`}
        detalle={contar(r.calificacion.total, 'calificación', 'calificaciones')}
      />
      <Dato
        titulo="Incidencias resueltas"
        valor={`${numero(r.incidencias.resueltas)}/${numero(r.incidencias.reportadas)}`}
        detalle="reportadas por vecinos y conductores"
      />
      <Dato
        titulo="Cisternas disponibles hoy"
        valor={`${numero(r.flota.disponiblesHoy)}/${numero(r.flota.total)}`}
        detalle="operativas y con autorización sanitaria vigente"
      />
    </div>
  );
}

function Dato({ titulo, valor, detalle, tono }: { titulo: string; valor: string; detalle: string; tono?: 'bueno' | 'malo' }) {
  return (
    <div className={`tarjeta indicador${tono ? ` indicador-${tono}` : ''}`}>
      <span className="texto-suave">{titulo}</span>
      <strong>{valor}</strong>
      <small className="texto-suave">{detalle}</small>
    </div>
  );
}

function agruparPorFecha(rutas: CronogramaItem[]): [string, CronogramaItem[]][] {
  const grupos = new Map<string, CronogramaItem[]>();
  for (const r of rutas) {
    grupos.set(r.fecha, [...(grupos.get(r.fecha) ?? []), r]);
  }
  return [...grupos.entries()];
}
