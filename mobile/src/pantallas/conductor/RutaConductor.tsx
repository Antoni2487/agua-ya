// La ruta del conductor: iniciar, GPS cada ~15 s (RF04), viajes, entregas validadas por geocerca (RF05),
// incidencias (RF06) y finalizar. El GPS se envía con la app abierta y la pantalla encendida.
import * as Location from 'expo-location';
import { activateKeepAwakeAsync, deactivateKeepAwake } from 'expo-keep-awake';
import { useCallback, useEffect, useRef, useState } from 'react';
import { Alert, StyleSheet, View } from 'react-native';
import MapView, { Marker } from 'react-native-maps';
import { api, textoError } from '../../api/cliente';
import type { Parada, RutaDetalle, Viaje } from '../../api/tipos';
import { Aviso, Boton, Campo, Cargando, Etiqueta, Fila, Pantalla, Tarjeta, Texto, tecladoNumerico } from '../../componentes/ui';
import { color } from '../../tema';
import { distanciaMetros, ESTADO_PARADA, ESTADO_RUTA, hora, textoDistancia, TURNO } from '../../textos';

const CADA_MS = 15000;
const GEOCERCA_M = 50;
type Punto = { latitude: number; longitude: number };

export function RutaConductor({ rutaId, alVolver, alReportar }: {
  rutaId: number; alVolver: () => void; alReportar: (paradaId?: number) => void;
}) {
  const [detalle, setDetalle] = useState<RutaDetalle | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [exito, setExito] = useState<string | null>(null);
  const [ocupado, setOcupado] = useState<string | null>(null);
  const [yo, setYo] = useState<Punto | null>(null);
  const [ultimoEnvio, setUltimoEnvio] = useState<Date | null>(null);
  const [errorGps, setErrorGps] = useState<string | null>(null);
  const [litros, setLitros] = useState('10000');
  const ultimaPosicion = useRef<Punto | null>(null);

  const cargar = useCallback(async () => {
    try {
      const rutas = await api<RutaDetalle[]>('/api/conductor/rutas/hoy');
      setDetalle(rutas.find((r) => r.ruta.id === rutaId) ?? null);
      if (!rutas.some((r) => r.ruta.id === rutaId)) setError('Esta ruta ya no está asignada para hoy.');
    } catch (e) {
      setError(textoError(e));
    }
  }, [rutaId]);

  useEffect(() => { void cargar(); }, [cargar]);

  const enCurso = detalle?.ruta.estado === 'EN_CURSO';

  const enviarPosicion = useCallback(async () => {
    const p = ultimaPosicion.current;
    if (!p) return false;
    try {
      await api(`/api/conductor/rutas/${rutaId}/posicion`, {
        metodo: 'POST', cuerpo: { latitud: Math.round(p.latitude * 1e6) / 1e6, longitud: Math.round(p.longitude * 1e6) / 1e6 },
      });
      setUltimoEnvio(new Date()); setErrorGps(null);
      return true;
    } catch (e) {
      setErrorGps(textoError(e));
      return false;
    }
  }, [rutaId]);

  // GPS: se escucha el GPS todo el tiempo que la pantalla está abierta (para ver distancias) y,
  // con la ruta en curso, se envía la última posición cada 15 s con la pantalla encendida.
  useEffect(() => {
    let suscripcion: Location.LocationSubscription | null = null;
    let vivo = true;
    (async () => {
      const permiso = await Location.requestForegroundPermissionsAsync();
      if (!permiso.granted) { setErrorGps('Activa el permiso de ubicación: sin GPS no se pueden validar las entregas.'); return; }
      suscripcion = await Location.watchPositionAsync(
        { accuracy: Location.Accuracy.High, timeInterval: 5000, distanceInterval: 5 },
        (pos) => {
          if (!vivo) return;
          const p = { latitude: pos.coords.latitude, longitude: pos.coords.longitude };
          ultimaPosicion.current = p; setYo(p);
        },
      );
    })();
    return () => { vivo = false; suscripcion?.remove(); };
  }, []);

  useEffect(() => {
    if (!enCurso) return;
    void activateKeepAwakeAsync('ruta');
    void enviarPosicion();
    const t = setInterval(() => void enviarPosicion(), CADA_MS);
    return () => { clearInterval(t); void deactivateKeepAwake('ruta'); };
  }, [enCurso, enviarPosicion]);

  const accion = async (clave: string, hacer: () => Promise<unknown>, mensaje?: string) => {
    setError(null); setExito(null); setOcupado(clave);
    try {
      await hacer();
      await cargar();
      if (mensaje) setExito(mensaje);
    } catch (e) {
      setError(textoError(e));
    } finally {
      setOcupado(null);
    }
  };

  const iniciar = () => accion('iniciar', () => api(`/api/conductor/rutas/${rutaId}/iniciar`, { metodo: 'PATCH' }),
    'Ruta iniciada. Tu ubicación se envía cada 15 segundos: deja la app abierta.');

  const salirCargado = () => {
    const volumen = Number(litros);
    if (!Number.isInteger(volumen) || volumen <= 0) { setError('Escribe los litros que cargaste (solo números).'); return; }
    return accion('viaje', () => api(`/api/conductor/rutas/${rutaId}/viajes`, { metodo: 'POST', cuerpo: { volumenCargadoLitros: volumen } }),
      'Viaje registrado. Las entregas quedan asociadas a este viaje.');
  };

  const terminarViaje = (viaje: Viaje) => accion('viaje', () => api(`/api/conductor/viajes/${viaje.id}/finalizar`, { metodo: 'PATCH' }),
    'Viaje terminado. Si vuelves a cargar, registra un viaje nuevo.');

  // Antes de atender se manda la posición actual, así la geocerca valida con un dato fresco.
  const atender = (parada: Parada) => accion(`p${parada.id}`, async () => {
    await enviarPosicion();
    await api(`/api/conductor/paradas/${parada.id}/atender`, { metodo: 'PATCH' });
  }, `Entrega registrada: ${parada.nombre}.`);

  const noAtendida = (parada: Parada) => Alert.alert('¿No pudiste atender esta parada?', parada.nombre, [
    { text: 'Cancelar', style: 'cancel' },
    { text: 'Sí, no la atendí', style: 'destructive',
      onPress: () => void accion(`p${parada.id}`, () => api(`/api/conductor/paradas/${parada.id}/no-atendida`, { metodo: 'PATCH' }),
        `Marcada como no atendida: ${parada.nombre}.`) },
  ]);

  const finalizar = () => Alert.alert('¿Terminar la ruta?', 'Todas las paradas deben estar atendidas o marcadas como no atendidas.', [
    { text: 'Cancelar', style: 'cancel' },
    { text: 'Terminar ruta', onPress: () => void accion('fin', () => api(`/api/conductor/rutas/${rutaId}/finalizar`, { metodo: 'PATCH' }), 'Ruta completada. ¡Buen trabajo!') },
  ]);

  if (!detalle) {
    return <Pantalla titulo="Ruta" alVolver={alVolver}>{error ? <Aviso texto={error} /> : <Cargando />}</Pantalla>;
  }

  const { ruta, paradas, viajes } = detalle;
  const estado = ESTADO_RUTA[ruta.estado];
  const viajeAbierto = viajes.find((v) => v.estado === 'EN_REPARTO');
  const pendientes = paradas.filter((p) => p.estado === 'PENDIENTE').sort((a, b) => a.orden - b.orden);
  const hechas = paradas.filter((p) => p.estado !== 'PENDIENTE').sort((a, b) => a.orden - b.orden);
  const siguiente = pendientes[0];
  const puntos = paradas.map((p) => ({ latitude: Number(p.latitud), longitude: Number(p.longitud) }));
  const centro = siguiente ? { latitude: Number(siguiente.latitud), longitude: Number(siguiente.longitud) } : puntos[0] ?? yo;

  return (
    <Pantalla titulo={`${ruta.sectorNombre} · ${TURNO[ruta.turno]}`} alVolver={alVolver}
      pie={ruta.estado === 'PROGRAMADA'
        ? <Boton texto="Iniciar ruta" alPresionar={iniciar} cargando={ocupado === 'iniciar'} />
        : enCurso
          ? <Fila>
              <Boton texto="Reportar problema" variante="claro" alPresionar={() => alReportar()} estilo={{ flex: 1 }} />
              <Boton texto="Terminar ruta" alPresionar={finalizar} cargando={ocupado === 'fin'} estilo={{ flex: 1 }} />
            </Fila>
          : null}>
      <Tarjeta>
        <Fila><Etiqueta texto={estado.texto} tono={estado.tono} /><Etiqueta texto={`Cisterna ${ruta.cisternaPlaca ?? '—'}`} /></Fila>
        <Texto tipo="grande">{ruta.paradasAtendidas} de {ruta.totalParadas}</Texto>
        <Texto tipo="suave">paradas atendidas · llenado en {detalle.puntoLlenadoNombre}</Texto>
      </Tarjeta>

      <Aviso texto={error} />
      <Aviso texto={exito} tipo="bien" />

      {enCurso ? (
        <Tarjeta tono="claro">
          <Texto tipo="subtitulo">GPS</Texto>
          <Texto>{ultimoEnvio ? `Ubicación enviada a las ${hora(ultimoEnvio.toISOString())}. Se envía sola cada 15 segundos.` : 'Enviando tu ubicación…'}</Texto>
          {errorGps ? <Texto estilo={{ color: color.error, fontWeight: '700' }}>{errorGps}</Texto> : null}
        </Tarjeta>
      ) : errorGps ? <Aviso texto={errorGps} /> : null}

      {enCurso ? (
        <Tarjeta>
          <Texto tipo="subtitulo">Viaje</Texto>
          {viajeAbierto ? (
            <>
              <Texto>Viaje {viajeAbierto.nroViaje} en reparto: {viajeAbierto.volumenCargadoLitros?.toLocaleString('es-PE')} litros, {viajeAbierto.paradasAtendidas} entregas.</Texto>
              <Boton texto="Volví a cargar: terminar este viaje" variante="secundario" alPresionar={() => terminarViaje(viajeAbierto)} cargando={ocupado === 'viaje'} />
            </>
          ) : (
            <>
              <Texto tipo="suave">Antes de repartir, registra cuántos litros cargaste en {detalle.puntoLlenadoNombre}.</Texto>
              <Campo etiqueta="Litros cargados" valor={litros} alCambiar={(t) => setLitros(t.replace(/\D/g, ''))} teclado={tecladoNumerico} maximo={5} />
              <Boton texto="Salir cargado" alPresionar={() => void salirCargado()} cargando={ocupado === 'viaje'} />
            </>
          )}
        </Tarjeta>
      ) : null}

      {centro ? (
        <View style={estilos.mapa}>
          <MapView style={StyleSheet.absoluteFill} initialRegion={{ ...centro, latitudeDelta: 0.02, longitudeDelta: 0.02 }}
            showsUserLocation accessibilityLabel="Mapa de las paradas de la ruta">
            {paradas.map((p) => (
              <Marker key={p.id} coordinate={{ latitude: Number(p.latitud), longitude: Number(p.longitud) }}
                title={`${p.orden}. ${p.nombre}`} description={ESTADO_PARADA[p.estado].texto}
                pinColor={p.estado === 'PENDIENTE' ? (p.id === siguiente?.id ? color.error : color.acento) : color.tintaSuave} />
            ))}
          </MapView>
        </View>
      ) : null}

      {pendientes.length ? <Texto tipo="titulo">Por atender ({pendientes.length})</Texto> : null}
      {pendientes.map((p) => {
        const distancia = yo ? distanciaMetros(yo, { latitude: Number(p.latitud), longitude: Number(p.longitud) }) : null;
        const cerca = distancia !== null && distancia <= GEOCERCA_M;
        return (
          <Tarjeta key={p.id} estilo={p.id === siguiente?.id ? estilos.siguiente : undefined}>
            <Fila>
              <Texto tipo="subtitulo">{p.orden}. {p.nombre}</Texto>
              {p.prioridad === 'CRITICA' ? <Etiqueta texto="Prioridad: salud o institución" tono="error" /> : null}
            </Fila>
            {p.referencia ? <Texto tipo="suave">{p.referencia}</Texto> : null}
            {distancia !== null ? (
              <Texto estilo={{ fontWeight: '700', color: cerca ? color.bien : color.tinta }}>
                {cerca ? `Estás en la parada (${textoDistancia(distancia)})` : `A ${textoDistancia(distancia)}: acércate a menos de ${GEOCERCA_M} m para registrar la entrega`}
              </Texto>
            ) : null}
            {enCurso ? (
              <Fila>
                <Boton texto="Entregué el agua" alPresionar={() => void atender(p)} cargando={ocupado === `p${p.id}`} estilo={{ flex: 1 }} />
                <Boton texto="No pude" variante="secundario" alPresionar={() => noAtendida(p)} estilo={{ flex: 1 }} />
              </Fila>
            ) : null}
          </Tarjeta>
        );
      })}

      {hechas.length ? <Texto tipo="titulo">Hechas ({hechas.length})</Texto> : null}
      {hechas.map((p) => (
        <Tarjeta key={p.id} tono="claro">
          <Texto>{p.orden}. {p.nombre}</Texto>
          <Fila><Etiqueta texto={ESTADO_PARADA[p.estado].texto} tono={ESTADO_PARADA[p.estado].tono} /></Fila>
        </Tarjeta>
      ))}
    </Pantalla>
  );
}

const estilos = StyleSheet.create({
  mapa: { height: 280, borderRadius: 22, borderBottomLeftRadius: 0, overflow: 'hidden', backgroundColor: color.c5 },
  siguiente: { borderWidth: 3, borderColor: color.acento },
});
