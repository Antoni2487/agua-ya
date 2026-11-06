// Registro del vecino: DNI + celular → código SMS → ubicación de su casa (GPS, con mapa para corregir) → cuenta.
// Los nombres salen del DNI en el backend; el sector se asigna solo al más cercano.
import * as Location from 'expo-location';
import { useEffect, useState } from 'react';
import { StyleSheet, View } from 'react-native';
import MapView, { Marker } from 'react-native-maps';
import { api, establecerSesion, textoError } from '../../api/cliente';
import type { AuthResponse, SolicitarCodigoResponse } from '../../api/tipos';
import { Aviso, Boton, Campo, Cargando, Pantalla, Tarjeta, Texto, tecladoNumerico } from '../../componentes/ui';
import { color } from '../../tema';

const CHICLAYO = { latitude: -6.7714, longitude: -79.8409 };
const PRECISION_MAXIMA_M = 50; // el radio de la geocerca: más impreciso que eso, se pide corregir en el mapa

type Punto = { latitude: number; longitude: number };
const seis = (n: number) => Math.round(n * 1e6) / 1e6;

export function RegistroVecino({ alVolver }: { alVolver: () => void }) {
  const [paso, setPaso] = useState<1 | 2>(1);
  const [dni, setDni] = useState('');
  const [telefono, setTelefono] = useState('');
  const [codigo, setCodigo] = useState('');
  const [referencia, setReferencia] = useState('');
  const [envio, setEnvio] = useState<SolicitarCodigoResponse | null>(null);
  const [punto, setPunto] = useState<Punto | null>(null);
  const [ubicando, setUbicando] = useState(false);
  const [notaUbicacion, setNotaUbicacion] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  const pedirCodigo = async () => {
    setError(null);
    if (!/^\d{8}$/.test(dni)) { setError('El DNI tiene 8 números.'); return; }
    if (!/^9\d{8}$/.test(telefono)) { setError('El celular tiene 9 números y empieza con 9.'); return; }
    setEnviando(true);
    try {
      const r = await api<SolicitarCodigoResponse>('/api/auth/otp/solicitar', { metodo: 'POST', cuerpo: { dni, telefono }, autenticado: false });
      setEnvio(r);
      if (r.codigo) setCodigo(r.codigo);
      setPaso(2);
    } catch (e) {
      setError(textoError(e));
    } finally {
      setEnviando(false);
    }
  };

  const ubicar = async () => {
    setUbicando(true); setNotaUbicacion(null);
    try {
      const permiso = await Location.requestForegroundPermissionsAsync();
      if (!permiso.granted) {
        setPunto((p) => p ?? CHICLAYO);
        setNotaUbicacion('Sin permiso de ubicación: mueve el pin del mapa hasta tu casa.');
        return;
      }
      const pos = await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.High });
      setPunto({ latitude: pos.coords.latitude, longitude: pos.coords.longitude });
      const precision = pos.coords.accuracy ?? 999;
      setNotaUbicacion(precision > PRECISION_MAXIMA_M
        ? `El GPS tiene poca precisión (${Math.round(precision)} m). Mueve el pin exactamente a tu casa.`
        : 'Listo: tomamos la ubicación de tu celular. Si no es tu casa, mueve el pin.');
    } catch {
      setPunto((p) => p ?? CHICLAYO);
      setNotaUbicacion('No pudimos leer el GPS: mueve el pin del mapa hasta tu casa.');
    } finally {
      setUbicando(false);
    }
  };

  useEffect(() => { if (paso === 2 && !punto) void ubicar(); }, [paso]); // eslint-disable-line react-hooks/exhaustive-deps

  const registrar = async () => {
    setError(null);
    if (!/^\d{4,10}$/.test(codigo)) { setError('Escribe el código que te llegó por SMS.'); return; }
    if (!punto) { setError('Falta la ubicación de tu casa.'); return; }
    setEnviando(true);
    try {
      const sesion = await api<AuthResponse>('/api/auth/registro/vecino', {
        metodo: 'POST', autenticado: false,
        cuerpo: {
          dni, telefono, codigo, direccionReferencia: referencia.trim() || null,
          latitud: seis(punto.latitude), longitud: seis(punto.longitude),
        },
      });
      await establecerSesion(sesion);
    } catch (e) {
      setError(textoError(e));
    } finally {
      setEnviando(false);
    }
  };

  return (
    <Pantalla titulo={paso === 1 ? 'Crear mi cuenta (1 de 2)' : 'Crear mi cuenta (2 de 2)'}
      alVolver={paso === 1 ? alVolver : () => setPaso(1)}
      pie={paso === 1
        ? <Boton texto="Enviarme el código por SMS" alPresionar={pedirCodigo} cargando={enviando} />
        : <Boton texto="Crear mi cuenta" alPresionar={registrar} cargando={enviando} deshabilitado={!punto} />}>
      {paso === 1 ? (
        <>
          <Texto tipo="suave">Con tu DNI sacamos tu nombre. Te mandamos un código al celular para confirmar que es tuyo.</Texto>
          <Campo etiqueta="Tu DNI" valor={dni} alCambiar={(t) => setDni(t.replace(/\D/g, ''))} teclado={tecladoNumerico} maximo={8} autoFocus />
          <Campo etiqueta="Tu celular" ayuda="9 números, por ejemplo 987654321" valor={telefono}
            alCambiar={(t) => setTelefono(t.replace(/\D/g, ''))} teclado="phone-pad" maximo={9} />
        </>
      ) : (
        <>
          <Tarjeta tono="claro">
            <Texto>{envio?.mensaje ?? 'Te enviamos un código por SMS.'}</Texto>
            {envio?.codigo ? <Texto tipo="pequeno">Modo demostración: el código ya está escrito abajo.</Texto> : null}
          </Tarjeta>
          <Campo etiqueta="Código del SMS" valor={codigo} alCambiar={(t) => setCodigo(t.replace(/\D/g, ''))} teclado={tecladoNumerico} maximo={10} />

          <Texto tipo="subtitulo">¿Dónde está tu casa?</Texto>
          <Texto tipo="suave">Aquí llegará la cisterna. Mantén el dedo sobre el pin y muévelo si hace falta.</Texto>
          {ubicando && !punto ? <Cargando texto="Buscando tu ubicación…" /> : null}
          {punto ? (
            <View style={estilos.mapa}>
              <MapView style={StyleSheet.absoluteFill}
                initialRegion={{ ...punto, latitudeDelta: 0.004, longitudeDelta: 0.004 }}
                onPress={(e) => setPunto(e.nativeEvent.coordinate)}
                accessibilityLabel="Mapa para marcar tu casa. Toca el mapa o arrastra el pin.">
                <Marker coordinate={punto} draggable onDragEnd={(e) => setPunto(e.nativeEvent.coordinate)}
                  title="Mi casa" pinColor={color.acento} />
              </MapView>
            </View>
          ) : null}
          {notaUbicacion ? <Texto tipo="pequeno">{notaUbicacion}</Texto> : null}
          <Boton texto="Usar mi ubicación actual" variante="secundario" alPresionar={ubicar} cargando={ubicando} />
          <Campo etiqueta="Referencia (opcional)" ayuda="Por ejemplo: Mz. B Lt. 12, frente a la losa"
            valor={referencia} alCambiar={setReferencia} maximo={255} />
        </>
      )}
      <Aviso texto={error} />
    </Pantalla>
  );
}

const estilos = StyleSheet.create({
  mapa: { height: 280, borderRadius: 22, borderBottomLeftRadius: 0, overflow: 'hidden', backgroundColor: color.c5 },
});
