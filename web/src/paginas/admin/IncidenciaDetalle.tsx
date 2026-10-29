import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';

import { api } from '../../api/cliente';
import type { Incidencia, Reasignacion, SugerenciasReasignacion } from '../../api/tipos';
import { Cargando, MensajeError } from '../../componentes/Estado';
import {
  contar, ESTADOS_INCIDENCIA, ESTADOS_RUTA, fechaCorta, fechaHora, numero, ROLES, TIPOS_INCIDENCIA, TURNOS,
} from '../../utilidades';

export function IncidenciaDetalle() {
  const id = Number(useParams().id);
  const clienteConsultas = useQueryClient();
  // El resultado vive aquí: al reasignar, la incidencia deja de estar ABIERTA y el formulario desaparece.
  const [reasignacion, setReasignacion] = useState<Reasignacion | null>(null);
  const incidencia = useQuery({
    queryKey: ['incidencias', id],
    queryFn: ({ signal }) => api<Incidencia>(`/api/admin/incidencias/${id}`, { signal }),
  });

  const cerrar = useMutation({
    mutationFn: () => api<Incidencia>(`/api/admin/incidencias/${id}/cerrar`, { metodo: 'PATCH' }),
    onSuccess: (actualizada) => {
      clienteConsultas.setQueryData(['incidencias', id], actualizada);
      void clienteConsultas.invalidateQueries({ queryKey: ['incidencias'] });
    },
  });

  if (incidencia.isPending) return <Cargando />;
  if (incidencia.isError) return <MensajeError error={incidencia.error} reintentar={() => void incidencia.refetch()} />;
  const i = incidencia.data;
  const reasignable = i.estado === 'ABIERTA' && (i.estadoRuta === 'INTERRUMPIDA' || i.estadoRuta === 'EN_CURSO');

  return (
    <section>
      <Link to="/admin/incidencias" className="volver">
        ← Incidencias
      </Link>
      <div className="encabezado">
        <h1>
          {TIPOS_INCIDENCIA[i.tipo]} · {i.sectorNombre}
        </h1>
        <span className={`etiqueta etiqueta-incidencia-${i.estado.toLowerCase()}`}>{ESTADOS_INCIDENCIA[i.estado]}</span>
      </div>

      <dl className="tarjeta ficha">
        <dt>Reportó</dt>
        <dd>
          {ROLES[i.reportadoPorRol]} · {i.reportadoPorNombre}
        </dd>
        <dt>Fecha</dt>
        <dd>{fechaHora(i.creadoEn)}</dd>
        <dt>Ruta</dt>
        <dd>
          <Link to={`/admin/rutas/${i.rutaId}`}>Ver ruta</Link> · {ESTADOS_RUTA[i.estadoRuta]}
          {i.ordenParada !== null && ` · parada ${i.ordenParada}`}
        </dd>
        {i.descripcion && (
          <>
            <dt>Descripción</dt>
            <dd>{i.descripcion}</dd>
          </>
        )}
        {i.estado !== 'ABIERTA' && (
          <>
            <dt>Resuelta</dt>
            <dd>
              {i.resueltaPorNombre} · {i.resueltoEn && fechaHora(i.resueltoEn)}
              {i.rutaDestinoId && (
                <>
                  {' '}
                  · paradas movidas a <Link to={`/admin/rutas/${i.rutaDestinoId}`}>otra ruta</Link>
                </>
              )}
            </dd>
          </>
        )}
      </dl>

      {reasignacion && (
        <div className="alerta alerta-exito resultado" role="status">
          <span>
            {reasignacion.paradasReasignadas > 0
              ? `${contar(reasignacion.paradasReasignadas, 'parada pasó', 'paradas pasaron')} a la ruta destino`
              : 'Las paradas pendientes ya estaban en la ruta destino'}
            {reasignacion.paradasReasignadas > 0 && reasignacion.paradasYaEnDestino > 0 &&
              ` (${reasignacion.paradasYaEnDestino} ya estaban en ella)`}
            {reasignacion.paradasReasignadas > 0 && '. Se avisó a los vecinos'}.
          </span>
          <Link to={`/admin/rutas/${reasignacion.rutaDestinoId}`}>Ver ruta destino</Link>
        </div>
      )}

      {reasignable && <Reasignar incidencia={i} alReasignar={setReasignacion} />}

      {i.estado === 'ABIERTA' && (
        <div className="acciones-incidencia">
          {cerrar.isError && <MensajeError error={cerrar.error} />}
          <button type="button" className="boton boton-secundario" disabled={cerrar.isPending} onClick={() => cerrar.mutate()}>
            {cerrar.isPending ? 'Cerrando…' : 'Cerrar como resuelta'}
          </button>
        </div>
      )}
    </section>
  );
}

/** RF07: el administrador elige la ruta destino; el sistema solo ordena las candidatas (sin algoritmo automático). */
function Reasignar({ incidencia, alReasignar }: { incidencia: Incidencia; alReasignar: (r: Reasignacion) => void }) {
  const clienteConsultas = useQueryClient();
  const [destino, setDestino] = useState<number | null>(null);
  const sugerencias = useQuery({
    queryKey: ['incidencias', incidencia.id, 'sugerencias'],
    queryFn: ({ signal }) =>
      api<SugerenciasReasignacion>(`/api/admin/incidencias/${incidencia.id}/sugerencias-reasignacion`, { signal }),
  });

  const reasignar = useMutation({
    mutationFn: (rutaDestinoId: number) =>
      api<Reasignacion>(`/api/admin/incidencias/${incidencia.id}/reasignar`, { metodo: 'POST', cuerpo: { rutaDestinoId } }),
    onSuccess: (r) => {
      alReasignar(r);
      clienteConsultas.setQueryData(['incidencias', incidencia.id], r.incidencia);
      void clienteConsultas.invalidateQueries({ queryKey: ['incidencias'] });
      void clienteConsultas.invalidateQueries({ queryKey: ['ruta', r.rutaOrigenId] });
      void clienteConsultas.invalidateQueries({ queryKey: ['ruta', r.rutaDestinoId] });
      void clienteConsultas.invalidateQueries({ queryKey: ['rutas'] });
    },
  });

  return (
    <div className="tarjeta formulario">
      <h2>Reasignar paradas pendientes</h2>
      {sugerencias.isPending && <Cargando texto="Buscando rutas del sector…" />}
      {sugerencias.isError && <MensajeError error={sugerencias.error} />}
      {sugerencias.data && (
        <>
          <p className="texto-suave">
            {contar(sugerencias.data.paradasPendientes, 'parada pendiente pasará', 'paradas pendientes pasarán')} a la ruta que
            elijas (mismo sector). Primero aparecen las cisternas en ruta más cercanas.
          </p>
          {sugerencias.data.rutas.length === 0 ? (
            <div className="alerta alerta-aviso">
              No hay otra ruta del sector programada o en curso. <Link to="/admin/rutas">Programa una</Link> y vuelve aquí.
            </div>
          ) : (
            <fieldset className="candidatas">
              <legend className="texto-suave">Ruta destino</legend>
              {sugerencias.data.rutas.map((r) => (
                <label key={r.rutaId} className={`candidata${destino === r.rutaId ? ' elegida' : ''}`}>
                  <input type="radio" name="destino" value={r.rutaId} checked={destino === r.rutaId} onChange={() => setDestino(r.rutaId)} />
                  <span>
                    <strong>
                      {fechaCorta(r.fecha)} · {TURNOS[r.turno]}
                    </strong>{' '}
                    · {ESTADOS_RUTA[r.estado]}
                    <br />
                    <span className="texto-suave">
                      {r.cisternaPlaca ?? 'Sin cisterna'}
                      {r.capacidadLitros !== null && ` (${numero(r.capacidadLitros)} L)`} · {r.conductorNombre ?? 'sin conductor'} ·{' '}
                      {contar(r.paradasPendientes, 'parada pendiente', 'paradas pendientes')}
                      {r.distanciaKm !== null && ` · a ${r.distanciaKm.toFixed(1)} km`}
                    </span>
                  </span>
                </label>
              ))}
            </fieldset>
          )}
          {reasignar.isError && <MensajeError error={reasignar.error} />}
          <div className="acciones">
            <button
              type="button"
              className="boton"
              disabled={destino === null || reasignar.isPending}
              onClick={() => destino !== null && reasignar.mutate(destino)}
            >
              {reasignar.isPending ? 'Reasignando…' : 'Reasignar paradas'}
            </button>
          </div>
        </>
      )}
    </div>
  );
}
