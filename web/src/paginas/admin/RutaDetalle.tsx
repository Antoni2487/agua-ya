import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { lazy, Suspense, useState, type FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';

import { usePuntosLlenadoActivos } from '../../api/catalogos';
import { api } from '../../api/cliente';
import type { RutaDetalle as Detalle, SugerenciasAsignacion } from '../../api/tipos';
import { Cargando, MensajeError } from '../../componentes/Estado';
import {
  contar, ESTADOS_PARADA, ESTADOS_RUTA, fechaCorta, hora, numero, TIPOS_PARADA, TURNOS, volumen,
} from '../../utilidades';

const MapaParadas = lazy(() => import('../../componentes/MapaParadas').then((m) => ({ default: m.MapaParadas })));

export function RutaDetalle() {
  const id = Number(useParams().id);
  const detalle = useQuery({
    queryKey: ['ruta', id],
    queryFn: ({ signal }) => api<Detalle>(`/api/admin/rutas/${id}`, { signal }),
    refetchInterval: (q) => (q.state.data?.ruta.estado === 'EN_CURSO' ? 15_000 : false),
  });
  const puntos = usePuntosLlenadoActivos();

  if (detalle.isPending) return <Cargando />;
  if (detalle.isError) return <MensajeError error={detalle.error} reintentar={() => void detalle.refetch()} />;

  const { ruta, paradas, viajes } = detalle.data;
  const puntoLlenado = puntos.data?.find((p) => p.id === detalle.data.puntoLlenadoId);
  const criticas = paradas.filter((p) => p.prioridad === 'CRITICA').length;

  return (
    <section>
      <Link to={`/admin/rutas?fecha=${ruta.fechaProgramada}`} className="volver">
        ← Rutas del {fechaCorta(ruta.fechaProgramada)}
      </Link>
      <div className="encabezado">
        <h1>
          {ruta.sectorNombre} · {fechaCorta(ruta.fechaProgramada)} · {TURNOS[ruta.turno]}
        </h1>
        <span className={`etiqueta etiqueta-${ruta.estado.toLowerCase()}`}>{ESTADOS_RUTA[ruta.estado]}</span>
      </div>

      <div className="resumen-ruta">
        <Dato titulo="Paradas" valor={`${ruta.paradasAtendidas}/${ruta.totalParadas} atendidas`} detalle={contar(criticas, 'crítica', 'críticas')} />
        <Dato titulo="Cisterna" valor={ruta.cisternaPlaca ?? 'Sin asignar'} />
        <Dato titulo="Conductor" valor={ruta.conductorNombre ?? 'Sin asignar'} />
        <Dato titulo="Viajes" valor={`${viajes.length} de ${ruta.viajesPlanificados ?? '—'} planificados`} />
        <Dato titulo="Punto de llenado" valor={detalle.data.puntoLlenadoNombre ?? '—'} />
      </div>

      {ruta.estado === 'PROGRAMADA' && <Asignacion detalle={detalle.data} />}

      <h2>Recorrido</h2>
      <div className="recorrido">
        <Suspense fallback={<div className="mapa mapa-cargando"><Cargando texto="Cargando mapa…" /></div>}>
          <MapaParadas paradas={paradas} puntoLlenado={puntoLlenado} />
        </Suspense>
        <div className="tabla-contenedor tabla-paradas">
          <table className="tabla">
            <thead>
              <tr>
                <th>#</th>
                <th>Parada</th>
                <th>Tipo</th>
                <th>Estado</th>
                <th>Atendida</th>
              </tr>
            </thead>
            <tbody>
              {paradas.map((p) => (
                <tr key={p.id}>
                  <td>{p.orden}</td>
                  <td>
                    {p.nombre}
                    {p.prioridad === 'CRITICA' && <span className="etiqueta etiqueta-critica">Crítica</span>}
                    {p.referencia && <div className="texto-suave celda-secundaria">{p.referencia}</div>}
                  </td>
                  <td>{TIPOS_PARADA[p.tipo]}</td>
                  <td>
                    <span className={`etiqueta etiqueta-parada-${p.estado.toLowerCase()}`}>{ESTADOS_PARADA[p.estado]}</span>
                  </td>
                  <td>{p.viajeId ? `Viaje ${viajes.find((v) => v.id === p.viajeId)?.nroViaje ?? ''}` : '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {viajes.length > 0 && (
        <>
          <h2>Viajes</h2>
          <div className="tabla-contenedor">
            <table className="tabla">
              <thead>
                <tr>
                  <th>Viaje</th>
                  <th>Carga</th>
                  <th>Salida</th>
                  <th>Retorno</th>
                  <th>Paradas atendidas</th>
                </tr>
              </thead>
              <tbody>
                {viajes.map((v) => (
                  <tr key={v.id}>
                    <td>N.º {v.nroViaje}</td>
                    <td>{v.volumenCargadoLitros === null ? '—' : volumen(v.volumenCargadoLitros)}</td>
                    <td>{hora(v.salida)}</td>
                    <td>{v.estado === 'FINALIZADO' ? hora(v.retorno) : <em className="texto-suave">En reparto</em>}</td>
                    <td>{numero(v.paradasAtendidas)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}

      {ruta.estado === 'PROGRAMADA' && <Eliminar id={ruta.id} fecha={ruta.fechaProgramada} />}
    </section>
  );
}

/** RF03: el sistema sugiere y el administrador confirma (regla 2). */
function Asignacion({ detalle }: { detalle: Detalle }) {
  const { ruta } = detalle;
  const clienteConsultas = useQueryClient();
  const sugerencias = useQuery({
    queryKey: ['ruta', ruta.id, 'sugerencias'],
    queryFn: ({ signal }) => api<SugerenciasAsignacion>(`/api/admin/rutas/${ruta.id}/sugerencias-asignacion`, { signal }),
  });
  const [cisternaId, setCisternaId] = useState('');
  const [conductorId, setConductorId] = useState('');
  // null = usar la sugerencia de la cisterna; '' = el usuario borró el campo.
  const [viajes, setViajes] = useState<string | null>(null);

  // Por defecto, la primera sugerencia (o lo ya asignado).
  const cisternaElegida = cisternaId || String(ruta.cisternaId ?? sugerencias.data?.cisternas[0]?.id ?? '');
  const conductorElegido = conductorId || String(ruta.conductorId ?? sugerencias.data?.conductores[0]?.id ?? '');
  const cisterna = sugerencias.data?.cisternas.find((c) => String(c.id) === cisternaElegida);
  const viajesElegidos = viajes ?? String(cisterna?.viajesNecesarios ?? ruta.viajesPlanificados ?? '');

  const asignar = useMutation({
    mutationFn: () =>
      api<Detalle>(`/api/admin/rutas/${ruta.id}/asignar`, {
        metodo: 'PATCH',
        cuerpo: {
          cisternaId: Number(cisternaElegida),
          conductorId: Number(conductorElegido),
          viajesPlanificados: viajesElegidos ? Number(viajesElegidos) : undefined,
        },
      }),
    onSuccess: (nuevo) => {
      clienteConsultas.setQueryData(['ruta', ruta.id], nuevo);
      void clienteConsultas.invalidateQueries({ queryKey: ['rutas'] });
      void clienteConsultas.invalidateQueries({ queryKey: ['ruta', ruta.id, 'sugerencias'] });
      setCisternaId('');
      setConductorId('');
      setViajes(null);
    },
  });

  function enviar(e: FormEvent) {
    e.preventDefault();
    asignar.mutate();
  }

  return (
    <form className="tarjeta formulario" onSubmit={enviar} aria-label="Asignar cisterna y conductor">
      <h2>{ruta.cisternaId ? 'Cambiar asignación' : 'Asignar cisterna y conductor'}</h2>
      {sugerencias.isPending && <Cargando texto="Buscando cisternas y conductores disponibles…" />}
      {sugerencias.isError && <MensajeError error={sugerencias.error} />}
      {sugerencias.data && (
        <>
          <p className="texto-suave">
            El sector necesita {volumen(sugerencias.data.litrosPorVisita)} por visita (máx. {sugerencias.data.maxViajesDiarios}{' '}
            viajes al día). Solo aparecen cisternas operativas, con autorización sanitaria vigente y libres en este turno.
          </p>
          {sugerencias.data.cisternas.length === 0 && (
            <div className="alerta alerta-aviso">No hay cisternas disponibles para esta fecha y turno.</div>
          )}
          {sugerencias.data.conductores.length === 0 && (
            <div className="alerta alerta-aviso">No hay conductores libres en esta fecha y turno.</div>
          )}
          <div className="campos">
            <label>
              Cisterna
              <select value={cisternaElegida} onChange={(e) => { setCisternaId(e.target.value); setViajes(null); }}>
                {sugerencias.data.cisternas.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.placa} · {numero(c.capacidadLitros)} L · {c.viajesNecesarios} viajes
                    {c.excedeMaximoDiario ? ' ⚠ excede máx.' : ''}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Conductor
              <select value={conductorElegido} onChange={(e) => setConductorId(e.target.value)}>
                {sugerencias.data.conductores.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.nombres} {c.apellidos} · turno {TURNOS[c.turno].toLowerCase()}
                    {c.distanciaKm !== null ? ` · a ${c.distanciaKm.toFixed(1)} km` : ''}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Viajes planificados
              <input
                type="number"
                min={1}
                max={50}
                value={viajesElegidos}
                onChange={(e) => setViajes(e.target.value)}
              />
            </label>
          </div>
          {cisterna?.excedeMaximoDiario && (
            <div className="alerta alerta-aviso">
              Con esta cisterna el sector necesita {cisterna.viajesNecesarios} viajes, más que el máximo diario: habrá déficit.
            </div>
          )}
          {asignar.isError && <MensajeError error={asignar.error} />}
          {asignar.isSuccess && <div className="alerta alerta-exito" role="status">Asignación guardada.</div>}
          <div className="acciones">
            <button type="submit" className="boton" disabled={!cisternaElegida || !conductorElegido || asignar.isPending}>
              {asignar.isPending ? 'Guardando…' : 'Confirmar asignación'}
            </button>
          </div>
        </>
      )}
    </form>
  );
}

function Eliminar({ id, fecha }: { id: number; fecha: string }) {
  const clienteConsultas = useQueryClient();
  const navegar = useNavigate();
  const eliminar = useMutation({
    mutationFn: () => api<void>(`/api/admin/rutas/${id}`, { metodo: 'DELETE' }),
    onSuccess: () => {
      void clienteConsultas.invalidateQueries({ queryKey: ['rutas'] });
      clienteConsultas.removeQueries({ queryKey: ['ruta', id] });
      navegar(`/admin/rutas?fecha=${fecha}`);
    },
  });
  return (
    <div className="zona-peligro">
      {eliminar.isError && <MensajeError error={eliminar.error} />}
      <button
        type="button"
        className="boton boton-peligro"
        disabled={eliminar.isPending}
        onClick={() => {
          if (window.confirm('¿Eliminar esta ruta y sus paradas? Solo es posible porque aún no inició.')) eliminar.mutate();
        }}
      >
        Eliminar ruta
      </button>
    </div>
  );
}

function Dato({ titulo, valor, detalle }: { titulo: string; valor: string; detalle?: string }) {
  return (
    <div className="tarjeta indicador">
      <span className="texto-suave">{titulo}</span>
      <strong className="dato-texto">{valor}</strong>
      {detalle && <small className="texto-suave">{detalle}</small>}
    </div>
  );
}
