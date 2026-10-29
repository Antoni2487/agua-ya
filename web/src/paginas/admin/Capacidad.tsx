import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';

import { api } from '../../api/cliente';
import type { CapacidadDetalle } from '../../api/tipos';
import { Cargando, MensajeError } from '../../componentes/Estado';
import { fechaCorta, hoyEnLima, numero, volumen } from '../../utilidades';

/**
 * RF02: viajes y cisternas que necesita cada sector (fórmula SUNASS) frente a la flota disponible en la fecha.
 * La capacidad de cisterna se puede simular para evaluar comprar o alquilar unidades más grandes.
 */
export function Capacidad() {
  const [params, setParams] = useSearchParams();
  const fecha = params.get('fecha') ?? hoyEnLima();
  const simulada = params.get('capacidadLitros') ?? '';
  const [borrador, setBorrador] = useState(simulada);

  const capacidad = useQuery({
    queryKey: ['capacidad', fecha, simulada],
    queryFn: ({ signal }) =>
      api<CapacidadDetalle>('/api/admin/capacidad', { parametros: { fecha, capacidadLitros: simulada }, signal }),
    placeholderData: keepPreviousData,
  });

  function cambiar(cambios: Record<string, string>) {
    const siguiente = new URLSearchParams(params);
    Object.entries(cambios).forEach(([k, v]) => (v ? siguiente.set(k, v) : siguiente.delete(k)));
    setParams(siguiente, { replace: true });
  }

  const c = capacidad.data;
  return (
    <section>
      <h1>Planificación de capacidad</h1>
      <p className="texto-suave intro">
        Viajes = ⌈unidades de uso × dotación × días entre visitas ÷ capacidad de la cisterna⌉ · cisternas = ⌈viajes ÷ máximo de
        viajes diarios⌉. Se comparan con las cisternas operativas y con autorización sanitaria vigente en la fecha.
      </p>
      <form
        className="filtros"
        onSubmit={(e) => {
          e.preventDefault();
          cambiar({ capacidadLitros: borrador });
        }}
      >
        <label>
          Fecha
          <input type="date" value={fecha} onChange={(e) => cambiar({ fecha: e.target.value })} />
        </label>
        <label>
          Simular capacidad de cisterna (L)
          <input
            type="number"
            min={1000}
            step={1000}
            placeholder={c ? `Promedio actual: ${numero(c.capacidadReferenciaLitros)}` : ''}
            value={borrador}
            onChange={(e) => setBorrador(e.target.value)}
          />
        </label>
        <div className="filtros-acciones">
          <button type="submit" className="boton boton-secundario">
            Simular
          </button>
          {simulada && (
            <button type="button" className="boton boton-secundario" onClick={() => { setBorrador(''); cambiar({ capacidadLitros: '' }); }}>
              Quitar simulación
            </button>
          )}
        </div>
      </form>

      {capacidad.isPending && <Cargando />}
      {capacidad.isError && <MensajeError error={capacidad.error} reintentar={() => void capacidad.refetch()} />}
      {c && (
        <>
          <div className={`alerta ${c.hayDeficit ? 'alerta-error' : 'alerta-exito'}`} role="status">
            {c.hayDeficit
              ? `Faltan ${c.deficit} cisternas el ${fechaCorta(c.fecha)} para atender todos los sectores activos.`
              : `La flota alcanza para todos los sectores activos el ${fechaCorta(c.fecha)}.`}
            {simulada && ` (Simulación con cisternas de ${numero(Number(simulada))} L.)`}
          </div>
          <div className="indicadores capacidad-indicadores">
            <Dato titulo="Cisternas necesarias" valor={numero(c.cisternasNecesarias)} />
            <Dato titulo="Disponibles" valor={numero(c.cisternasDisponibles)} detalle={`de ${numero(c.flota.total)} en la flota`} />
            <Dato titulo="No operativas" valor={numero(c.flota.noOperativas)} detalle="mantenimiento o averiadas" />
            <Dato titulo="Autorización vencida" valor={numero(c.flota.autorizacionVencida)} detalle="no pueden asignarse" />
            <Dato titulo="Capacidad de referencia" valor={volumen(c.capacidadReferenciaLitros)} detalle={simulada ? 'simulada' : 'promedio de las disponibles'} />
          </div>

          <h2>Por sector</h2>
          <div className="tabla-contenedor">
            <table className="tabla">
              <thead>
                <tr>
                  <th>Sector</th>
                  <th>Unidades de uso</th>
                  <th>Días entre visitas</th>
                  <th>Litros por visita</th>
                  <th>Viajes</th>
                  <th>Máx. diario</th>
                  <th>Cisternas</th>
                  <th>Déficit si fuera el único</th>
                </tr>
              </thead>
              <tbody>
                {c.sectores.map((s) => (
                  <tr key={s.sectorId}>
                    <td>
                      {s.nombre}
                      <div className="texto-suave celda-secundaria">
                        {s.distrito}
                        {s.esZonaAlta && ' · zona alta'}
                      </div>
                    </td>
                    <td>{numero(s.unidadesUso)}</td>
                    <td>{s.frecuenciaDias}</td>
                    <td>{volumen(s.litrosPorVisita)}</td>
                    <td className={s.viajesNecesarios > s.maxViajesDiarios ? 'intento-fuera' : ''}>{s.viajesNecesarios}</td>
                    <td>{s.maxViajesDiarios}</td>
                    <td>{s.cisternasNecesarias}</td>
                    <td className={s.deficit > 0 ? 'intento-fuera' : ''}>{s.deficit}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </section>
  );
}

function Dato({ titulo, valor, detalle }: { titulo: string; valor: string; detalle?: string }) {
  return (
    <div className="tarjeta indicador">
      <span className="texto-suave">{titulo}</span>
      <strong>{valor}</strong>
      {detalle && <small className="texto-suave">{detalle}</small>}
    </div>
  );
}
