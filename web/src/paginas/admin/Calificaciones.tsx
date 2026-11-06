import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { Link, useSearchParams } from 'react-router-dom';

import { useSectoresActivos } from '../../api/catalogos';
import { api } from '../../api/cliente';
import type { Calificacion, Pagina, ResumenCalificaciones } from '../../api/tipos';
import { Cargando, MensajeError } from '../../componentes/Estado';
import { Paginacion } from '../../componentes/Paginacion';
import { contar, fechaCorta, numero, TURNOS } from '../../utilidades';

const estrellas = (n: number) => '★'.repeat(n) + '☆'.repeat(5 - n);

/** RF08: lo que opinan los vecinos de cada entrega; el promedio por conductor ayuda a detectar incumplimientos. */
export function Calificaciones() {
  const [params, setParams] = useSearchParams();
  const sectorId = params.get('sectorId') ?? '';
  const page = Number(params.get('page') ?? 0);
  const sectores = useSectoresActivos();

  const resumen = useQuery({
    queryKey: ['calificaciones', 'resumen', sectorId],
    queryFn: ({ signal }) => api<ResumenCalificaciones>('/api/admin/calificaciones/resumen', { parametros: { sectorId }, signal }),
  });
  const lista = useQuery({
    queryKey: ['calificaciones', 'lista', sectorId, page],
    queryFn: ({ signal }) =>
      api<Pagina<Calificacion>>('/api/admin/calificaciones', { parametros: { sectorId, page, size: 20 }, signal }),
    placeholderData: keepPreviousData,
  });

  function filtrar(cambios: Record<string, string>) {
    const siguiente = new URLSearchParams(params);
    Object.entries(cambios).forEach(([k, v]) => (v ? siguiente.set(k, v) : siguiente.delete(k)));
    if (!('page' in cambios)) siguiente.delete('page');
    setParams(siguiente, { replace: true });
  }

  const r = resumen.data;
  const maximo = r ? Math.max(1, ...Object.values(r.distribucion)) : 1;
  return (
    <section>
      <h1>Calificaciones</h1>
      <div className="filtros">
        <label>
          Sector
          <select value={sectorId} onChange={(e) => filtrar({ sectorId: e.target.value })}>
            <option value="">Todos</option>
            {sectores.data?.map((s) => (
              <option key={s.id} value={s.id}>
                {s.nombre}
              </option>
            ))}
          </select>
        </label>
      </div>

      {resumen.isPending && <Cargando />}
      {resumen.isError && <MensajeError error={resumen.error} reintentar={() => void resumen.refetch()} />}
      {r && (
        <div className="calificaciones-resumen">
          <div className="tarjeta indicador">
            <span className="texto-suave">Promedio</span>
            <strong>{r.promedio === null ? '—' : `${r.promedio.toFixed(1)} ★`}</strong>
            <small className="texto-suave">{contar(r.total, 'calificación', 'calificaciones')}</small>
          </div>
          <div className="tarjeta distribucion" aria-label="Distribución por estrellas">
            {[5, 4, 3, 2, 1].map((n) => {
              const cantidad = r.distribucion[String(n)] ?? 0;
              return (
                <div key={n} className="fila-distribucion">
                  <span>{n} ★</span>
                  <span className="barra-avance">
                    <span style={{ width: `${(cantidad / maximo) * 100}%`, background: n <= 2 ? 'var(--rojo)' : 'var(--verde)' }} />
                  </span>
                  <span>{numero(cantidad)}</span>
                </div>
              );
            })}
          </div>
          <div className="tarjeta">
            <h2 className="titulo-tarjeta">Por conductor (del peor al mejor)</h2>
            {r.conductores.length === 0 && <p className="texto-suave">Aún sin calificaciones.</p>}
            <table className="tabla tabla-compacta">
              <tbody>
                {r.conductores.map((c) => (
                  <tr key={c.conductorId}>
                    <td>{c.nombre}</td>
                    <td className={c.promedio < 3 ? 'intento-fuera' : ''}>{c.promedio.toFixed(1)} ★</td>
                    <td className="texto-suave">{contar(c.total, 'calificación', 'calificaciones')}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <h2>Comentarios recientes</h2>
      {lista.isPending && <Cargando />}
      {lista.isError && <MensajeError error={lista.error} reintentar={() => void lista.refetch()} />}
      {lista.data?.contenido.length === 0 && <p className="texto-suave">No hay calificaciones todavía.</p>}
      {lista.data && lista.data.contenido.length > 0 && (
        <>
          <ul className="comentarios">
            {lista.data.contenido.map((c) => (
              <li key={c.id} className="tarjeta">
                <div className="comentario-cabecera">
                  <span className="estrellas" aria-label={`${c.estrellas} de 5 estrellas`}>
                    {estrellas(c.estrellas)}
                  </span>
                  <span className="texto-suave">
                    {c.sectorNombre} · <Link to={`/admin/rutas/${c.rutaId}`}>{fechaCorta(c.fecha)} {TURNOS[c.turno].toLowerCase()}</Link>
                  </span>
                </div>
                {c.comentario ? <p>{c.comentario}</p> : <p className="texto-suave">Sin comentario</p>}
                <small className="texto-suave">
                  {c.vecinoNombre} · cisterna {c.cisternaPlaca ?? '—'} · {c.conductorNombre ?? '—'}
                </small>
              </li>
            ))}
          </ul>
          <Paginacion pagina={lista.data} alCambiar={(p) => filtrar({ page: String(p) })} />
        </>
      )}
    </section>
  );
}
