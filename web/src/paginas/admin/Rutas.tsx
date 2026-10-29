import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, type FormEvent } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';

import { usePuntosLlenadoActivos, useSectoresActivos } from '../../api/catalogos';
import { api } from '../../api/cliente';
import type { EstadoRuta, Pagina, RutaDetalle, RutaResumen, Turno } from '../../api/tipos';
import { Cargando, MensajeError } from '../../componentes/Estado';
import { Paginacion } from '../../componentes/Paginacion';
import { ESTADOS_RUTA, fechaCorta, hoyEnLima, TURNOS } from '../../utilidades';

/** Rutas por fecha, estado y sector (RF01). Los filtros viven en la URL para poder compartir el enlace. */
export function Rutas() {
  const [params, setParams] = useSearchParams();
  const fecha = params.get('fecha') ?? hoyEnLima();
  const estado = (params.get('estado') ?? '') as EstadoRuta | '';
  const sectorId = params.get('sectorId') ?? '';
  const page = Number(params.get('page') ?? 0);
  const [programando, setProgramando] = useState(false);
  const sectores = useSectoresActivos();

  const rutas = useQuery({
    queryKey: ['rutas', { fecha, estado, sectorId, page }],
    queryFn: ({ signal }) =>
      api<Pagina<RutaResumen>>('/api/admin/rutas', {
        parametros: { fecha, estado, sectorId, page, size: 20, sort: 'turno' },
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
      <div className="encabezado">
        <h1>Rutas</h1>
        <button type="button" className="boton" onClick={() => setProgramando((v) => !v)} aria-expanded={programando}>
          {programando ? 'Cerrar' : 'Programar ruta'}
        </button>
      </div>

      {programando && <FormularioProgramar fechaInicial={fecha} alCancelar={() => setProgramando(false)} />}

      <div className="filtros">
        <label>
          Fecha
          <input type="date" value={fecha} onChange={(e) => filtrar({ fecha: e.target.value })} />
        </label>
        <label>
          Estado
          <select value={estado} onChange={(e) => filtrar({ estado: e.target.value })}>
            <option value="">Todos</option>
            {Object.entries(ESTADOS_RUTA).map(([valor, etiqueta]) => (
              <option key={valor} value={valor}>
                {etiqueta}
              </option>
            ))}
          </select>
        </label>
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

      {rutas.isPending && <Cargando />}
      {rutas.isError && <MensajeError error={rutas.error} reintentar={() => void rutas.refetch()} />}
      {rutas.data && rutas.data.contenido.length === 0 && (
        <p className="texto-suave">No hay rutas para {fechaCorta(fecha)} con esos filtros.</p>
      )}
      {rutas.data && rutas.data.contenido.length > 0 && (
        <>
          <div className="tabla-contenedor">
            <table className="tabla">
              <thead>
                <tr>
                  <th>Sector</th>
                  <th>Turno</th>
                  <th>Estado</th>
                  <th>Cisterna</th>
                  <th>Conductor</th>
                  <th>Viajes</th>
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
                    <td>{r.viajesPlanificados ?? '—'}</td>
                    <td>
                      {r.paradasAtendidas}/{r.totalParadas}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Paginacion pagina={rutas.data} alCambiar={(p) => filtrar({ page: String(p) })} />
        </>
      )}
    </section>
  );
}

/** RF01: genera las paradas (críticas primero, luego hogares o puntos de entrega) y lleva al detalle para asignar. */
function FormularioProgramar({ fechaInicial, alCancelar }: { fechaInicial: string; alCancelar: () => void }) {
  const hoy = hoyEnLima();
  const sectores = useSectoresActivos();
  const puntos = usePuntosLlenadoActivos();
  const clienteConsultas = useQueryClient();
  const navegar = useNavigate();
  const [sectorId, setSectorId] = useState('');
  const [fecha, setFecha] = useState(fechaInicial < hoy ? hoy : fechaInicial);
  const [turno, setTurno] = useState<Turno>('MANANA');
  const [puntoLlenadoId, setPuntoLlenadoId] = useState('');
  const puntoElegido = puntoLlenadoId || (puntos.data?.length === 1 ? String(puntos.data[0]!.id) : '');

  const programar = useMutation({
    mutationFn: () =>
      api<RutaDetalle>('/api/admin/rutas', {
        metodo: 'POST',
        cuerpo: { sectorId: Number(sectorId), fecha, turno, puntoLlenadoId: Number(puntoElegido) },
      }),
    onSuccess: (detalle) => {
      void clienteConsultas.invalidateQueries({ queryKey: ['rutas'] });
      clienteConsultas.setQueryData(['ruta', detalle.ruta.id], detalle);
      navegar(`/admin/rutas/${detalle.ruta.id}`);
    },
  });

  function enviar(e: FormEvent) {
    e.preventDefault();
    programar.mutate();
  }

  return (
    <form className="tarjeta formulario" onSubmit={enviar} aria-label="Programar ruta">
      <h2>Programar ruta</h2>
      <p className="texto-suave">
        Se generan las paradas del sector: primero las instituciones críticas y luego los hogares o puntos de entrega, en
        recorrido desde el punto de llenado. Después asignas cisterna y conductor.
      </p>
      <div className="campos">
        <label>
          Sector
          <select required value={sectorId} onChange={(e) => setSectorId(e.target.value)}>
            <option value="">Elegir…</option>
            {sectores.data?.map((s) => (
              <option key={s.id} value={s.id}>
                {s.nombre} · {s.distrito}
              </option>
            ))}
          </select>
        </label>
        <label>
          Fecha
          <input type="date" required min={hoy} value={fecha} onChange={(e) => setFecha(e.target.value)} />
        </label>
        <label>
          Turno
          <select value={turno} onChange={(e) => setTurno(e.target.value as Turno)}>
            <option value="MANANA">{TURNOS.MANANA}</option>
            <option value="TARDE">{TURNOS.TARDE}</option>
          </select>
        </label>
        <label>
          Punto de llenado
          <select required value={puntoElegido} onChange={(e) => setPuntoLlenadoId(e.target.value)}>
            <option value="">Elegir…</option>
            {puntos.data?.map((p) => (
              <option key={p.id} value={p.id}>
                {p.nombre}
              </option>
            ))}
          </select>
        </label>
      </div>
      {programar.isError && <MensajeError error={programar.error} />}
      <div className="acciones">
        <button type="button" className="boton boton-secundario" onClick={alCancelar}>
          Cancelar
        </button>
        <button type="submit" className="boton" disabled={!sectorId || !puntoElegido || !fecha || programar.isPending}>
          {programar.isPending ? 'Programando…' : 'Programar'}
        </button>
      </div>
    </form>
  );
}
