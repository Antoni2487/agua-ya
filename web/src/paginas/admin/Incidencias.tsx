import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { Link, useSearchParams } from 'react-router-dom';

import { api } from '../../api/cliente';
import type { Incidencia, Pagina } from '../../api/tipos';
import { Cargando, MensajeError } from '../../componentes/Estado';
import { Paginacion } from '../../componentes/Paginacion';
import { ESTADOS_INCIDENCIA, fechaHora, ROLES, TIPOS_INCIDENCIA } from '../../utilidades';

/** RF06: incidencias de conductores y vecinos; las nuevas llegan por WebSocket y refrescan esta lista. */
export function Incidencias() {
  const [params, setParams] = useSearchParams();
  const estado = params.get('estado') ?? 'ABIERTA';
  const tipo = params.get('tipo') ?? '';
  const page = Number(params.get('page') ?? 0);

  const incidencias = useQuery({
    queryKey: ['incidencias', { estado, tipo, page }],
    queryFn: ({ signal }) =>
      api<Pagina<Incidencia>>('/api/admin/incidencias', {
        parametros: { estado: estado === 'TODAS' ? '' : estado, tipo, page, size: 20 },
        signal,
      }),
    placeholderData: keepPreviousData,
  });

  function filtrar(cambios: Record<string, string>) {
    const siguiente = new URLSearchParams(params);
    Object.entries(cambios).forEach(([k, v]) => (v ? siguiente.set(k, v) : siguiente.delete(k)));
    if (!('page' in cambios)) siguiente.delete('page');
    setParams(siguiente, { replace: true });
  }

  return (
    <section>
      <h1>Incidencias</h1>
      <div className="filtros">
        <label>
          Estado
          <select value={estado} onChange={(e) => filtrar({ estado: e.target.value })}>
            {Object.entries(ESTADOS_INCIDENCIA).map(([v, e]) => (
              <option key={v} value={v}>
                {e}s
              </option>
            ))}
            <option value="TODAS">Todas</option>
          </select>
        </label>
        <label>
          Tipo
          <select value={tipo} onChange={(e) => filtrar({ tipo: e.target.value })}>
            <option value="">Todos</option>
            {Object.entries(TIPOS_INCIDENCIA).map(([v, e]) => (
              <option key={v} value={v}>
                {e}
              </option>
            ))}
          </select>
        </label>
      </div>

      {incidencias.isPending && <Cargando />}
      {incidencias.isError && <MensajeError error={incidencias.error} reintentar={() => void incidencias.refetch()} />}
      {incidencias.data?.contenido.length === 0 && (
        <p className="texto-suave">{estado === 'ABIERTA' ? 'No hay incidencias abiertas.' : 'No hay incidencias con esos filtros.'}</p>
      )}
      {incidencias.data && incidencias.data.contenido.length > 0 && (
        <>
          <div className="tabla-contenedor">
            <table className="tabla">
              <thead>
                <tr>
                  <th>Fecha</th>
                  <th>Tipo</th>
                  <th>Sector</th>
                  <th>Reportó</th>
                  <th>Estado</th>
                </tr>
              </thead>
              <tbody>
                {incidencias.data.contenido.map((i) => (
                  <tr key={i.id}>
                    <td>{fechaHora(i.creadoEn)}</td>
                    <td>
                      <Link to={`/admin/incidencias/${i.id}`}>{TIPOS_INCIDENCIA[i.tipo]}</Link>
                      {i.estadoRuta === 'INTERRUMPIDA' && <span className="etiqueta etiqueta-interrumpida">Ruta detenida</span>}
                    </td>
                    <td>{i.sectorNombre}</td>
                    <td>
                      {ROLES[i.reportadoPorRol]} · {i.reportadoPorNombre}
                    </td>
                    <td>
                      <span className={`etiqueta etiqueta-incidencia-${i.estado.toLowerCase()}`}>{ESTADOS_INCIDENCIA[i.estado]}</span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Paginacion pagina={incidencias.data} alCambiar={(p) => filtrar({ page: String(p) })} />
        </>
      )}
    </section>
  );
}
