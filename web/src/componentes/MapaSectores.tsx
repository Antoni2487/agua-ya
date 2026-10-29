import 'leaflet/dist/leaflet.css';

import { latLngBounds } from 'leaflet';
import { useEffect } from 'react';
import { CircleMarker, MapContainer, Popup, TileLayer, Tooltip, useMap } from 'react-leaflet';

import type { SectorTransparencia } from '../api/tipos';
import { fechaCorta, porcentaje, TURNOS } from '../utilidades';

/** Centro aproximado de Chiclayo, para cuando no hay sectores que mostrar. */
const CHICLAYO: [number, number] = [-6.7714, -79.8409];

const LEYENDA = [
  { color: '#1f7a4d', texto: '≥ 90 %' },
  { color: '#c58a00', texto: '70–89 %' },
  { color: '#b42318', texto: '< 70 %' },
  { color: '#8a96a3', texto: 'Sin rutas cerradas en el periodo' },
];

/** Color por cumplimiento: verde ≥ 90 %, ámbar ≥ 70 %, rojo por debajo; gris sin datos del periodo. */
export function colorCumplimiento(valor: number | null): string {
  if (valor === null) return '#8a96a3';
  if (valor >= 90) return '#1f7a4d';
  if (valor >= 70) return '#c58a00';
  return '#b42318';
}

const posicion = (s: SectorTransparencia): [number, number] => [Number(s.latCentro), Number(s.lngCentro)];

/** Encuadra los sectores visibles; se reencuadra al filtrar por distrito. */
function Encuadre({ sectores }: { sectores: SectorTransparencia[] }) {
  const mapa = useMap();
  const clave = sectores.map((s) => s.sectorId).join(',');
  useEffect(() => {
    if (sectores.length === 0) return;
    mapa.invalidateSize(); // por si el contenedor cambió de tamaño (rotar el celular, achicar la ventana)
    mapa.fitBounds(latLngBounds(sectores.map(posicion)), { padding: [40, 40], maxZoom: 14 });
  }, [mapa, clave]);
  return null;
}

/**
 * Sectores en OpenStreetMap. Solo el centro de cada sector (regla 9: nunca viviendas ni paradas),
 * coloreado según el cumplimiento del periodo.
 */
export function MapaSectores({ sectores }: { sectores: SectorTransparencia[] }) {
  return (
    <figure className="mapa-figura">
      <MapContainer center={CHICLAYO} zoom={12} scrollWheelZoom={false} className="mapa" aria-label="Mapa de sectores atendidos">
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <Encuadre sectores={sectores} />
        {sectores.map((s) => (
          <CircleMarker
            key={s.sectorId}
            center={posicion(s)}
            radius={11}
            pathOptions={{ color: '#fff', weight: 2, fillColor: colorCumplimiento(s.cumplimientoPorcentaje), fillOpacity: 0.95 }}
          >
            <Tooltip direction="top" offset={[0, -8]}>
              {s.nombre}
            </Tooltip>
            <Popup>
              <strong>{s.nombre}</strong>
              <br />
              {s.distrito}
              {s.esZonaAlta && ' · zona alta'}
              <br />
              Próxima atención:{' '}
              {s.proximaAtencion ? `${fechaCorta(s.proximaAtencion.fecha)} (${TURNOS[s.proximaAtencion.turno]})` : 'sin programar'}
              <br />
              Cumplimiento: {porcentaje(s.cumplimientoPorcentaje)}
            </Popup>
          </CircleMarker>
        ))}
      </MapContainer>
      <figcaption className="leyenda">
        <span className="texto-suave">Cumplimiento:</span>
        {LEYENDA.map((l) => (
          <span key={l.texto}>
            <i style={{ background: l.color }} aria-hidden="true" /> {l.texto}
          </span>
        ))}
      </figcaption>
    </figure>
  );
}
