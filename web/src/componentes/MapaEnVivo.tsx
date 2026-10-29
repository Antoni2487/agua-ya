import 'leaflet/dist/leaflet.css';

import { latLngBounds } from 'leaflet';
import { Fragment, useEffect, useRef } from 'react';
import { CircleMarker, MapContainer, Polyline, Popup, TileLayer, Tooltip, useMap } from 'react-leaflet';

import type { Posicion, RutaResumen } from '../api/tipos';
import { hace } from '../utilidades';

export interface RutaEnMapa {
  ruta: RutaResumen;
  posiciones: Posicion[];
  sinSenal: boolean;
}

/** Encuadra las cisternas la primera vez que hay posiciones; luego respeta el zoom del usuario. */
function EncuadreInicial({ puntos }: { puntos: [number, number][] }) {
  const mapa = useMap();
  const hecho = useRef(false);
  useEffect(() => {
    if (hecho.current || puntos.length === 0) return;
    hecho.current = true;
    mapa.invalidateSize();
    mapa.fitBounds(latLngBounds(puntos), { padding: [50, 50], maxZoom: 15 });
  }, [mapa, puntos]);
  return null;
}

/** Cisternas en ruta (RF04): última posición, estela del recorrido y aviso si dejó de enviar GPS. */
export function MapaEnVivo({ rutas, seleccionada, alSeleccionar }: {
  rutas: RutaEnMapa[];
  seleccionada: number | null;
  alSeleccionar: (rutaId: number) => void;
}) {
  const ultimas = rutas.flatMap((r) => (r.posiciones.length ? [r.posiciones[r.posiciones.length - 1]!] : []));
  return (
    <MapContainer center={[-6.7714, -79.8409]} zoom={12} className="mapa" aria-label="Mapa de cisternas en ruta">
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      <EncuadreInicial puntos={ultimas.map((p) => [Number(p.latitud), Number(p.longitud)])} />
      {rutas.map(({ ruta, posiciones, sinSenal }) => {
        if (posiciones.length === 0) return null;
        const ultima = posiciones[posiciones.length - 1]!;
        const elegida = seleccionada === ruta.id;
        return (
          <Fragment key={ruta.id}>
            <Polyline
              positions={posiciones.map((p) => [Number(p.latitud), Number(p.longitud)] as [number, number])}
              pathOptions={{ color: elegida ? '#08507f' : '#0b6aa8', weight: elegida ? 4 : 3, opacity: elegida ? 0.9 : 0.5 }}
            />
            <CircleMarker
              center={[Number(ultima.latitud), Number(ultima.longitud)]}
              radius={elegida ? 12 : 10}
              pathOptions={{ color: '#fff', weight: 3, fillColor: sinSenal ? '#b42318' : '#0b6aa8', fillOpacity: 1 }}
              eventHandlers={{ click: () => alSeleccionar(ruta.id) }}
            >
              <Tooltip permanent direction="right" offset={[10, 0]} className="placa-mapa">
                {ruta.cisternaPlaca ?? `Ruta ${ruta.id}`}
              </Tooltip>
              <Popup>
                <strong>{ruta.sectorNombre}</strong>
                <br />
                {ruta.conductorNombre}
                <br />
                Última posición {hace(ultima.registradoEn)}
              </Popup>
            </CircleMarker>
          </Fragment>
        );
      })}
    </MapContainer>
  );
}
