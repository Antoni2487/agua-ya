import 'leaflet/dist/leaflet.css';

import { latLngBounds } from 'leaflet';
import { useEffect } from 'react';
import { CircleMarker, MapContainer, Polyline, Popup, TileLayer, Tooltip, useMap } from 'react-leaflet';

import type { EstadoParada, Parada, Posicion, PuntoLlenado, Recorrido } from '../api/tipos';
import { ESTADOS_PARADA, hora, TIPOS_PARADA } from '../utilidades';

const COLOR_ESTADO: Record<EstadoParada, string> = {
  PENDIENTE: '#0b6aa8',
  ATENDIDA: '#1f7a4d',
  NO_ATENDIDA: '#b42318',
  REASIGNADA: '#8a96a3',
};

function Encuadre({ puntos }: { puntos: [number, number][] }) {
  const mapa = useMap();
  const clave = puntos.map((p) => p.join()).join(';');
  useEffect(() => {
    if (puntos.length === 0) return;
    mapa.invalidateSize();
    mapa.fitBounds(latLngBounds(puntos), { padding: [30, 30], maxZoom: 16 });
  }, [mapa, clave]);
  return null;
}

/**
 * Recorrido planificado (paradas numeradas por estado, desde el punto de llenado) y, si la ruta ya inició, el
 * recorrido real del GPS con la última posición y los intentos de atender fuera de la geocerca. Solo para el admin.
 */
export function MapaParadas({ paradas, puntoLlenado, posiciones = [], intentos = [] }: {
  paradas: Parada[];
  puntoLlenado?: PuntoLlenado;
  posiciones?: Posicion[];
  intentos?: Recorrido['intentosFallidos'];
}) {
  const real = posiciones.map((p) => [Number(p.latitud), Number(p.longitud)] as [number, number]);
  const ultima = posiciones.at(-1);
  const recorrido: [number, number][] = [
    ...(puntoLlenado ? [[Number(puntoLlenado.latitud), Number(puntoLlenado.longitud)] as [number, number]] : []),
    ...paradas.map((p) => [Number(p.latitud), Number(p.longitud)] as [number, number]),
  ];
  return (
    <MapContainer center={recorrido[0] ?? [-6.7714, -79.8409]} zoom={14} scrollWheelZoom={false} className="mapa" aria-label="Mapa del recorrido">
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      {/* Encuadra las paradas; la planta suele estar lejos y las amontonaría (la línea indica hacia dónde queda). */}
      <Encuadre puntos={paradas.length ? recorrido.slice(puntoLlenado ? 1 : 0) : recorrido} />
      <Polyline positions={recorrido} pathOptions={{ color: '#0b6aa8', weight: 2, opacity: 0.5, dashArray: '6 6' }} />
      {real.length > 1 && <Polyline positions={real} pathOptions={{ color: '#c58a00', weight: 4, opacity: 0.85 }} />}
      {intentos.filter((i) => i.latitud !== null && i.longitud !== null).map((i) => (
        <CircleMarker
          key={`${i.paradaId}-${i.registradoEn}`}
          center={[Number(i.latitud), Number(i.longitud)]}
          radius={6}
          pathOptions={{ color: '#b42318', weight: 2, fillColor: '#fff', fillOpacity: 1, dashArray: '2 2' }}
        >
          <Tooltip>
            Intento fuera de geocerca · parada {i.ordenParada} · {i.distanciaMetros !== null ? `${Math.round(Number(i.distanciaMetros))} m` : '—'} ·{' '}
            {hora(i.registradoEn)}
          </Tooltip>
        </CircleMarker>
      ))}
      {ultima && (
        <CircleMarker
          center={real.at(-1)!}
          radius={10}
          pathOptions={{ color: '#fff', weight: 3, fillColor: '#c58a00', fillOpacity: 1 }}
        >
          <Tooltip>Cisterna · {hora(ultima.registradoEn)}</Tooltip>
        </CircleMarker>
      )}
      {puntoLlenado && (
        <CircleMarker
          center={recorrido[0]!}
          radius={9}
          pathOptions={{ color: '#fff', weight: 2, fillColor: '#08507f', fillOpacity: 1 }}
        >
          <Tooltip>Punto de llenado: {puntoLlenado.nombre}</Tooltip>
        </CircleMarker>
      )}
      {paradas.map((p) => (
        <CircleMarker
          key={p.id}
          center={[Number(p.latitud), Number(p.longitud)]}
          radius={p.prioridad === 'CRITICA' ? 11 : 9}
          pathOptions={{ color: '#fff', weight: 2, fillColor: COLOR_ESTADO[p.estado], fillOpacity: 0.95 }}
        >
          <Tooltip permanent direction="center" className="numero-parada">
            {p.orden}
          </Tooltip>
          <Popup>
            <strong>
              {p.orden}. {p.nombre}
            </strong>
            <br />
            {TIPOS_PARADA[p.tipo]}
            {p.prioridad === 'CRITICA' && ' · crítica'} · {ESTADOS_PARADA[p.estado]}
            {p.referencia && (
              <>
                <br />
                {p.referencia}
              </>
            )}
          </Popup>
        </CircleMarker>
      ))}
    </MapContainer>
  );
}
