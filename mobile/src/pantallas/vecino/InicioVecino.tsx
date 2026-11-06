// Inicio del vecino: cuándo llega su cisterna, el camión en el mapa mientras reparte, calificar y reportar.
import { useCallback, useEffect, useRef, useState } from 'react';
import { StyleSheet, View } from 'react-native';
import MapView, { Marker } from 'react-native-maps';
import { api, ErrorApi, textoError } from '../../api/cliente';
import type { Atencion, AtencionVecino, Posicion, VecinoPerfil } from '../../api/tipos';
import { Aviso, Boton, Cargando, Etiqueta, Fila, Pantalla, Tarjeta, Texto } from '../../componentes/ui';
import { color } from '../../tema';
import { cuando, distanciaMetros, ESTADO_PARADA, ESTADO_RUTA, hora, textoDistancia, TURNO } from '../../textos';

const CADA_MS = 15000; // el conductor envía su posición cada ~15 s

export function InicioVecino({ alCalificar, alReportar, alVerReportes, alSalir }: {
  alCalificar: (paradaId: number) => void;
  alReportar: (rutaId: number, paradaId?: number) => void;
  alVerReportes: () => void;
  alSalir: () => void;
}) {
  const [perfil, setPerfil] = useState<VecinoPerfil | null>(null);
  const [atencion, setAtencion] = useState<AtencionVecino | null>(null);
  const [posicion, setPosicion] = useState<Posicion | null>(null);
  const [sinPosicion, setSinPosicion] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [cargando, setCargando] = useState(true);
  const mapa = useRef<MapView>(null);

  const cargar = useCallback(async () => {
    setError(null);
    try {
      const [p, a] = await Promise.all([
        api<VecinoPerfil>('/api/vecino/me'),
        api<AtencionVecino>('/api/vecino/proxima-atencion'),
      ]);
      setPerfil(p); setAtencion(a);
    } catch (e) {
      setError(textoError(e));
    } finally {
      setCargando(false);
    }
  }, []);

  useEffect(() => {
    void cargar();
    const t = setInterval(() => void cargar(), 60000);
    return () => clearInterval(t);
  }, [cargar]);

  const enCamino = atencion?.proxima?.estado === 'EN_CURSO' ? atencion.proxima : null;

  // Mientras la cisterna reparte en su sector, se pide su posición cada 15 s
  useEffect(() => {
    if (!enCamino) { setPosicion(null); return; }
    let vivo = true;
    const pedir = async () => {
      try {
        const p = await api<Posicion>(`/api/vecino/rutas/${enCamino.rutaId}/posicion`);
        if (vivo) { setPosicion(p); setSinPosicion(false); }
      } catch (e) {
        if (vivo && e instanceof ErrorApi && e.status !== 0) setSinPosicion(true);
      }
    };
    void pedir();
    const t = setInterval(pedir, CADA_MS);
    return () => { vivo = false; clearInterval(t); };
  }, [enCamino?.rutaId]); // eslint-disable-line react-hooks/exhaustive-deps

  const casa = perfil ? { latitude: Number(perfil.latitud), longitude: Number(perfil.longitud) } : null;
  const camion = posicion ? { latitude: Number(posicion.latitud), longitude: Number(posicion.longitud) } : null;

  useEffect(() => {
    if (casa && camion) mapa.current?.fitToCoordinates([casa, camion], { edgePadding: { top: 60, right: 60, bottom: 60, left: 60 }, animated: true });
  }, [camion?.latitude, camion?.longitude]); // eslint-disable-line react-hooks/exhaustive-deps

  const rutaParaReportar = atencion?.proxima ?? atencion?.ultima ?? null;

  return (
    <Pantalla titulo={perfil ? `Hola, ${perfil.nombres.split(' ')[0]}` : 'Tu agua'}
      pie={<Fila>
        <Boton texto="Actualizar" variante="secundario" alPresionar={() => { setCargando(true); void cargar(); }} estilo={{ flex: 1 }} />
        <Boton texto="Salir" variante="secundario" alPresionar={alSalir} estilo={{ flex: 1 }} />
      </Fila>}>
      {cargando && !atencion ? <Cargando texto="Buscando tu próxima cisterna…" /> : null}
      <Aviso texto={error} />
      {perfil?.sector ? <Texto tipo="suave">Tu sector: {perfil.sector.nombre}, {perfil.sector.distrito}</Texto> : null}

      {atencion && !atencion.proxima ? (
        <Tarjeta tono="claro">
          <Texto tipo="subtitulo">Aún no hay una cisterna programada</Texto>
          <Texto>Cuando EPSEL programe la visita a tu sector, la verás aquí y te avisaremos.</Texto>
        </Tarjeta>
      ) : null}

      {atencion?.proxima ? (
        <TarjetaAtencion atencion={atencion.proxima} titulo={enCamino ? 'La cisterna está en camino' : 'Tu próxima cisterna'} destacada />
      ) : null}

      {enCamino && casa ? (
        <View style={estilos.mapa}>
          <MapView ref={mapa} style={StyleSheet.absoluteFill}
            initialRegion={{ ...casa, latitudeDelta: 0.02, longitudeDelta: 0.02 }}
            accessibilityLabel={camion ? `Mapa: la cisterna está a ${textoDistancia(distanciaMetros(casa, camion))} de tu casa` : 'Mapa de tu casa'}>
            <Marker coordinate={casa} title="Tu casa" pinColor={color.acento} />
            {camion ? <Marker coordinate={camion} title="Cisterna" description={`Última señal ${hora(posicion!.registradoEn)}`} pinColor={color.bien} /> : null}
          </MapView>
        </View>
      ) : null}
      {enCamino && camion && casa ? (
        <Texto tipo="subtitulo">La cisterna está a {textoDistancia(distanciaMetros(casa, camion))} de tu casa (señal de las {hora(posicion!.registradoEn)}).</Texto>
      ) : null}
      {enCamino && sinPosicion ? <Texto tipo="suave">Aún no recibimos la ubicación del camión. Se actualiza sola.</Texto> : null}

      {atencion?.ultima ? (
        <>
          <TarjetaAtencion atencion={atencion.ultima} titulo="Tu última cisterna" />
          {atencion.ultima.miParada?.estado === 'ATENDIDA' && atencion.ultima.miParada.miCalificacion == null ? (
            <Boton texto="Calificar la entrega" alPresionar={() => alCalificar(atencion.ultima!.miParada!.paradaId)} />
          ) : null}
          {atencion.ultima.miParada?.miCalificacion != null ? (
            <Texto tipo="suave">Calificaste esta entrega con {atencion.ultima.miParada.miCalificacion} de 5 estrellas. ¡Gracias!</Texto>
          ) : null}
        </>
      ) : null}

      {rutaParaReportar ? (
        <Boton texto="Reportar un problema" variante="claro"
          alPresionar={() => alReportar(rutaParaReportar.rutaId, rutaParaReportar.miParada?.paradaId)} />
      ) : null}
      <Boton texto="Ver mis reportes" variante="secundario" alPresionar={alVerReportes} />
    </Pantalla>
  );
}

function TarjetaAtencion({ atencion, titulo, destacada = false }: { atencion: Atencion; titulo: string; destacada?: boolean }) {
  const estado = ESTADO_RUTA[atencion.estado];
  const mi = atencion.miParada;
  return (
    <Tarjeta tono={destacada ? 'blanco' : 'claro'} estilo={destacada ? estilos.destacada : undefined}>
      <Texto tipo="subtitulo">{titulo}</Texto>
      <Texto tipo="grande">{cuando(atencion.fecha)}</Texto>
      <Fila>
        <Etiqueta texto={TURNO[atencion.turno]} />
        <Etiqueta texto={estado.texto} tono={estado.tono} />
      </Fila>
      {mi ? (
        <>
          <Texto>Tu parada: {mi.nombre}{mi.referencia ? ` · ${mi.referencia}` : ''}</Texto>
          <Fila><Etiqueta texto={ESTADO_PARADA[mi.estado].texto} tono={ESTADO_PARADA[mi.estado].tono} /></Fila>
          {atencion.estado === 'EN_CURSO' && mi.estado === 'PENDIENTE' ? (
            <Texto tipo="subtitulo">{mi.pendientesAntes === 0 ? '¡Eres la siguiente parada!' : `Faltan ${mi.pendientesAntes} paradas antes que la tuya.`}</Texto>
          ) : null}
        </>
      ) : null}
      <Texto tipo="pequeno">Avance de la ruta: {atencion.paradasAtendidas} de {atencion.totalParadas} paradas atendidas.</Texto>
    </Tarjeta>
  );
}

const estilos = StyleSheet.create({
  mapa: { height: 300, borderRadius: 22, borderBottomLeftRadius: 0, overflow: 'hidden', backgroundColor: color.c5 },
  destacada: { borderWidth: 3, borderColor: color.acento },
});
