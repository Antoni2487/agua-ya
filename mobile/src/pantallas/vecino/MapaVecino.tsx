// Cisterna en vivo (Figma 03): el camión y la casa del vecino en el mapa, y una hoja abajo con cuántas paradas
// faltan. Por privacidad (regla 9) solo se ve la cisterna mientras reparte en su sector y su propia parada.
import { useEffect, useRef, useState, type ReactNode } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import MapView, { Marker } from 'react-native-maps';
import { api, ErrorApi } from '../../api/cliente';
import type { Posicion } from '../../api/tipos';
import { Icono } from '../../componentes/iconos';
import { Boton, Etiqueta, Fila, Pantalla, Texto } from '../../componentes/ui';
import { color, fuente, medida } from '../../tema';
import { distanciaMetros, haceCuanto, teToca, textoDistancia, TURNO_FRASE } from '../../textos';
import type { DatosVecino } from './VecinoApp';

const CADA_MS = 15000; // el conductor envía su posición cada ~15 s
const CHICLAYO = { latitude: -6.7714, longitude: -79.8409 };

export function MapaVecino({ datos, barra, alVolver }: { datos: DatosVecino; barra: ReactNode; alVolver: () => void }) {
  const { perfil, atencion } = datos;
  const proxima = atencion?.proxima ?? null;
  const enCamino = proxima?.estado === 'EN_CURSO' ? proxima : null;
  const [posicion, setPosicion] = useState<Posicion | null>(null);
  const [ahora, setAhora] = useState(Date.now());
  const mapa = useRef<MapView>(null);

  useEffect(() => {
    if (!enCamino) { setPosicion(null); return; }
    let vivo = true;
    const pedir = async () => {
      try {
        const p = await api<Posicion>(`/api/vecino/rutas/${enCamino.rutaId}/posicion`);
        if (vivo) setPosicion(p);
      } catch (e) {
        if (vivo && e instanceof ErrorApi && e.status === 409) void datos.recargar(); // la ruta terminó
      }
    };
    void pedir();
    const t = setInterval(pedir, CADA_MS);
    const reloj = setInterval(() => setAhora(Date.now()), 1000);
    return () => { vivo = false; clearInterval(t); clearInterval(reloj); };
  }, [enCamino?.rutaId]); // eslint-disable-line react-hooks/exhaustive-deps

  const casa = perfil ? { latitude: Number(perfil.latitud), longitude: Number(perfil.longitud) } : null;
  const camion = posicion ? { latitude: Number(posicion.latitud), longitude: Number(posicion.longitud) } : null;

  useEffect(() => {
    if (casa && camion) mapa.current?.fitToCoordinates([casa, camion], { edgePadding: { top: 80, right: 80, bottom: 80, left: 80 }, animated: true });
  }, [camion?.latitude, camion?.longitude]); // eslint-disable-line react-hooks/exhaustive-deps

  const mi = enCamino?.miParada;
  const distancia = casa && camion ? textoDistancia(distanciaMetros(casa, camion)) : null;
  const descripcionMapa = camion && distancia ? `Mapa: la cisterna está a ${distancia} de tu casa.` : 'Mapa con la ubicación de tu casa.';

  return (
    <Pantalla titulo="Cisterna en vivo" alVolver={alVolver} barra={barra} sinDesplazar>
      <View style={estilos.mapa}>
        <MapView ref={mapa} style={StyleSheet.absoluteFill} accessibilityLabel={descripcionMapa}
          initialRegion={{ ...(casa ?? CHICLAYO), latitudeDelta: 0.015, longitudeDelta: 0.015 }}>
          {casa ? (
            <Marker coordinate={casa} title="Tu casa" anchor={{ x: 0.5, y: 1 }}>
              <Icono nombre="ubicacion" tamano={40} color={color.tuCasa} />
            </Marker>
          ) : null}
          {camion ? (
            <Marker coordinate={camion} title={`Cisterna ${enCamino?.cisternaPlaca ?? ''}`} anchor={{ x: 0.5, y: 0.5 }}>
              <View style={estilos.camion}><Icono nombre="camion" tamano={22} color={color.blanco} /></View>
            </Marker>
          ) : null}
        </MapView>
      </View>

      <View style={estilos.hoja}>
        {enCamino ? (
          <>
            <Fila estilo={{ justifyContent: 'space-between' }}>
              <Etiqueta texto="En camino" tono="marino" icono="camion" />
              <Text style={estilos.suave}>{posicion ? `Actualizado ${haceCuanto(posicion.registradoEn, ahora)}` : 'Esperando señal…'}</Text>
            </Fila>
            <Texto tipo="subtitulo">
              {mi?.estado === 'ATENDIDA' ? 'Ya te atendieron en esta ruta'
                : mi && mi.estado === 'PENDIENTE' ? (mi.pendientesAntes === 0 ? '¡Eres la siguiente parada!' : `Faltan ${mi.pendientesAntes} paradas antes de la tuya`)
                : 'La cisterna está repartiendo en tu sector'}
            </Texto>
            <Texto>
              {camion && distancia ? `La cisterna ${enCamino.cisternaPlaca ?? ''} está a ${distancia} de tu casa.` : 'Aún no recibimos la ubicación del camión. Se actualiza sola cada 15 segundos.'}
            </Texto>
            <View style={{ gap: 8 }}>
              <Fila estilo={{ gap: 8 }}><View style={[estilos.punto, { backgroundColor: color.tinta }]} /><Text style={estilos.leyenda}>Cisterna {enCamino.cisternaPlaca ?? ''}: círculo con camión</Text></Fila>
              <Fila estilo={{ gap: 8 }}><View style={[estilos.punto, { backgroundColor: color.tuCasa }]} /><Text style={estilos.leyenda}>Tu casa: pin naranja</Text></Fila>
            </View>
          </>
        ) : proxima ? (
          <>
            <Etiqueta texto="Programada" tono="info" icono="calendario" />
            <Texto tipo="subtitulo">La cisterna aún no sale</Texto>
            <Texto>{teToca(proxima.fecha)}, {TURNO_FRASE[proxima.turno]}. Cuando la cisterna empiece a repartir, la verás moverse aquí.</Texto>
          </>
        ) : (
          <>
            <Etiqueta texto="Sin programar" tono="neutro" icono="calendario" />
            <Texto tipo="subtitulo">No hay una cisterna en camino</Texto>
            <Texto>Cuando EPSEL programe la visita a tu sector y la cisterna salga, la verás aquí en vivo.</Texto>
          </>
        )}
        <Boton texto="Volver al inicio" variante="secundario" alPresionar={alVolver} />
      </View>
    </Pantalla>
  );
}

const estilos = StyleSheet.create({
  mapa: { flex: 1, minHeight: 260, backgroundColor: color.hundido },
  hoja: {
    gap: 12, padding: 20, backgroundColor: color.blanco, borderTopLeftRadius: medida.radio, borderTopRightRadius: medida.radio,
    marginTop: -16,
  },
  camion: {
    width: 44, height: 44, borderRadius: 22, backgroundColor: color.tinta, alignItems: 'center', justifyContent: 'center',
    borderWidth: 3, borderColor: color.blanco,
  },
  punto: { width: 14, height: 14, borderRadius: 7 },
  suave: { fontSize: 15, fontFamily: fuente.cuerpo, color: color.tintaSuave },
  leyenda: { fontSize: 15, fontFamily: fuente.cuerpo, color: color.cuerpo },
});
