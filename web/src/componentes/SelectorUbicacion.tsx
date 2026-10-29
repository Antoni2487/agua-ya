import 'leaflet/dist/leaflet.css';

import { useEffect } from 'react';
import { CircleMarker, MapContainer, TileLayer, useMap, useMapEvents } from 'react-leaflet';

/** Centro aproximado de Chiclayo, para cuando aún no hay ubicación ni sector. */
const CHICLAYO: [number, number] = [-6.7714, -79.8409];

/** Mismo redondeo que `Coordenada.normalizar` en el backend: 6 decimales (~10 cm). */
const redondear = (valor: number) => Math.round(valor * 1e6) / 1e6;

function AlHacerClic({ alElegir }: { alElegir: (latitud: number, longitud: number) => void }) {
  useMapEvents({ click: (e) => alElegir(redondear(e.latlng.lat), redondear(e.latlng.lng)) });
  return null;
}

/** Recentra al elegir otro sector mientras no hay ubicación marcada. */
function Centrar({ centro, marcado }: { centro: [number, number]; marcado: boolean }) {
  const mapa = useMap();
  const [lat, lng] = centro;
  useEffect(() => {
    if (!marcado) mapa.setView([lat, lng], 15);
  }, [mapa, lat, lng, marcado]);
  return null;
}

/** Mapa para marcar una ubicación con un clic; los campos de latitud y longitud siguen editables. */
export function SelectorUbicacion({
  posicion,
  centro,
  alElegir,
}: {
  posicion: [number, number] | null;
  centro: [number, number] | null;
  alElegir: (latitud: number, longitud: number) => void;
}) {
  const inicio = posicion ?? centro ?? CHICLAYO;
  return (
    <figure className="mapa-figura">
      <MapContainer center={inicio} zoom={posicion || centro ? 15 : 12} scrollWheelZoom={false} className="mapa mapa-selector" aria-label="Mapa para marcar la ubicación">
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <AlHacerClic alElegir={alElegir} />
        <Centrar centro={centro ?? CHICLAYO} marcado={posicion !== null} />
        {posicion && <CircleMarker center={posicion} radius={9} pathOptions={{ color: '#fff', weight: 2, fillColor: '#0b5cad', fillOpacity: 1 }} />}
      </MapContainer>
      <figcaption className="texto-suave">Haz clic en el mapa para marcar la ubicación.</figcaption>
    </figure>
  );
}
