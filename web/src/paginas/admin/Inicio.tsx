import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';

import { api } from '../../api/cliente';
import type { Capacidad, Pagina, RutaResumen } from '../../api/tipos';
import { Cargando, MensajeError } from '../../componentes/Estado';
import { Icono, type NombreIcono } from '../../componentes/Icono';
import { ESTADOS_RUTA, fechaCorta, hoyEnLima, TURNOS } from '../../utilidades';

/** Resumen del día: alerta de capacidad (RF02) y rutas de hoy con su avance. */
export function Inicio() {
  const hoy = hoyEnLima();
  const capacidad = useQuery({
    queryKey: ['capacidad', hoy],
    queryFn: ({ signal }) => api<Capacidad>('/api/admin/capacidad', { parametros: { fecha: hoy }, signal }),
  });
  const rutas = useQuery({
    queryKey: ['rutas', { fecha: hoy }],
    queryFn: ({ signal }) =>
      api<Pagina<RutaResumen>>('/api/admin/rutas', { parametros: { fecha: hoy, size: 100 }, signal }),
    refetchInterval: 30_000,
  });

  return (
    <section>
      <header className="cabecera-pagina">
        <p className="sobretitulo">Resumen del día</p>
        <h1>Hoy · {fechaCorta(hoy)}</h1>
      </header>

      <h2>
        Capacidad <Link to="/admin/capacidad" className="enlace-titulo">ver detalle</Link>
      </h2>
      {capacidad.isPending && <Cargando />}
      {capacidad.isError && <MensajeError error={capacidad.error} reintentar={() => void capacidad.refetch()} />}
      {capacidad.data && (
        <div className="indicadores">
          <Indicador titulo="Cisternas necesarias" valor={capacidad.data.cisternasNecesarias} icono="capacidad" />
          <Indicador titulo="Cisternas disponibles" valor={capacidad.data.cisternasDisponibles} icono="cisterna" />
          <Indicador
            titulo="Déficit"
            valor={capacidad.data.deficit}
            icono={capacidad.data.hayDeficit ? 'alerta' : 'check'}
            tono={capacidad.data.hayDeficit ? 'malo' : 'bueno'}
          />
          <Indicador
            titulo="Fuera de servicio"
            valor={capacidad.data.flota.noOperativas + capacidad.data.flota.autorizacionVencida}
            icono="incidencias"
            detalle={`${capacidad.data.flota.noOperativas} no operativas · ${capacidad.data.flota.autorizacionVencida} con autorización vencida`}
          />
        </div>
      )}

      <h2>Rutas de hoy</h2>
      {rutas.isPending && <Cargando />}
      {rutas.isError && <MensajeError error={rutas.error} reintentar={() => void rutas.refetch()} />}
      {rutas.data && rutas.data.contenido.length === 0 && <p className="texto-suave">No hay rutas programadas para hoy.</p>}
      {rutas.data && rutas.data.contenido.length > 0 && (
        <div className="tabla-contenedor">
          <table className="tabla">
            <thead>
              <tr>
                <th>Sector</th>
                <th>Turno</th>
                <th>Estado</th>
                <th>Cisterna</th>
                <th>Conductor</th>
                <th>Avance</th>
              </tr>
            </thead>
            <tbody>
              {rutas.data.contenido.map((r) => (
                <tr key={r.id}>
                  <td>
                    <Link to={`/admin/rutas/${r.id}`}>{r.sectorNombre}</Link>
                  </td>
                  <td>{TURNOS[r.turno]}</td>
                  <td>
                    <span className={`etiqueta etiqueta-${r.estado.toLowerCase()}`}>{ESTADOS_RUTA[r.estado]}</span>
                  </td>
                  <td>{r.cisternaPlaca ?? <em className="texto-suave">Sin asignar</em>}</td>
                  <td>{r.conductorNombre ?? <em className="texto-suave">Sin asignar</em>}</td>
                  <td>
                    <span className="avance">
                      <span className="barra-avance" aria-hidden="true">
                        <span style={{ width: `${r.totalParadas ? (100 * r.paradasAtendidas) / r.totalParadas : 0}%` }} />
                      </span>
                      {r.paradasAtendidas}/{r.totalParadas}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

function Indicador({ titulo, valor, detalle, tono, icono }: {
  titulo: string; valor: number; detalle?: string; tono?: 'bueno' | 'malo'; icono?: NombreIcono;
}) {
  return (
    <div className={`tarjeta indicador${tono ? ` indicador-${tono}` : ''}`}>
      {icono && <span className="indicador-icono"><Icono nombre={icono} /></span>}
      <span className="texto-suave">{titulo}</span>
      <strong>{valor}</strong>
      {detalle && <small className="texto-suave">{detalle}</small>}
    </div>
  );
}
