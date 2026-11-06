// Piezas de interfaz comunes, con los estilos del Figma de AguaYa. Todo lo que se toca mide al menos 48 dp
// y tiene su etiqueta para el lector de pantalla (TalkBack / VoiceOver).
import { useState, type ReactNode } from 'react';
import {
  ActivityIndicator, Pressable, ScrollView, StyleSheet, Text, TextInput, View,
  type KeyboardTypeOptions, type StyleProp, type TextStyle, type ViewStyle,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { color, fuente, letra, medida, sombra } from '../tema';
import { Icono, type NombreIcono } from './iconos';

/**
 * Pantalla con barra superior (título y botón Volver), contenido desplazable y, opcional, un pie fijo con
 * acciones o la barra de pestañas del vecino.
 */
export function Pantalla({ titulo, alVolver, children, pie, barra, cabecera, sinDesplazar }: {
  titulo?: string; alVolver?: () => void; children: ReactNode; pie?: ReactNode; barra?: ReactNode;
  cabecera?: ReactNode; sinDesplazar?: boolean;
}) {
  const borde = useSafeAreaInsets();
  const abajo = barra ? 24 : borde.bottom + 32;
  return (
    <View style={[estilos.pantalla, { paddingTop: borde.top }]}>
      {cabecera ?? (titulo ? (
        <View style={estilos.cabecera}>
          {alVolver ? (
            <Pressable onPress={alVolver} accessibilityRole="button" accessibilityLabel="Volver"
              style={({ pressed }) => [estilos.volver, pressed && { opacity: 0.6 }]}>
              <Icono nombre="atras" />
            </Pressable>
          ) : null}
          <Text style={estilos.cabeceraTitulo} accessibilityRole="header" numberOfLines={2}>{titulo}</Text>
        </View>
      ) : null)}
      {sinDesplazar ? <View style={{ flex: 1 }}>{children}</View> : (
        <ScrollView contentContainerStyle={[estilos.contenido, { paddingBottom: abajo }]} keyboardShouldPersistTaps="handled">
          {children}
        </ScrollView>
      )}
      {pie ? <View style={[estilos.pie, { paddingBottom: (barra ? 0 : borde.bottom) + 12 }]}>{pie}</View> : null}
      {barra}
    </View>
  );
}

type Variante = 'principal' | 'secundario' | 'peligro' | 'claro';

/** principal = azul lleno; secundario = contorno azul (como "Reportar un problema" del Figma). */
export function Boton({ texto, alPresionar, variante = 'principal', cargando = false, deshabilitado = false, estilo, etiqueta, icono }: {
  texto: string; alPresionar: () => void; variante?: Variante; cargando?: boolean; deshabilitado?: boolean;
  estilo?: StyleProp<ViewStyle>; etiqueta?: string; icono?: NombreIcono;
}) {
  const apagado = deshabilitado || cargando;
  const fondo = { principal: color.acento, secundario: color.blanco, peligro: color.error, claro: color.acentoSuave }[variante];
  const tinta = variante === 'principal' || variante === 'peligro' ? color.blanco : color.acento;
  return (
    <Pressable onPress={alPresionar} disabled={apagado} accessibilityRole="button"
      accessibilityLabel={etiqueta ?? texto} accessibilityState={{ disabled: apagado, busy: cargando }}
      style={({ pressed }) => [estilos.boton, { backgroundColor: fondo },
        variante === 'secundario' && estilos.botonBorde, apagado && { opacity: 0.55 }, pressed && { opacity: 0.85 }, estilo]}>
      {cargando ? <ActivityIndicator color={tinta} /> : (
        <View style={estilos.botonFila}>
          {icono ? <Icono nombre={icono} color={tinta} /> : null}
          <Text style={[estilos.botonTexto, { color: tinta }]}>{texto}</Text>
        </View>
      )}
    </Pressable>
  );
}

/** Campo con etiqueta arriba y ayuda (o error) debajo, como en el Figma. `secreto` agrega el botón Mostrar. */
export function Campo({ etiqueta, valor, alCambiar, ayuda, error, teclado, secreto, maximo, autoFocus, multilinea, contador, autoCompletar }: {
  etiqueta: string; valor: string; alCambiar: (t: string) => void; ayuda?: string; error?: string | null;
  teclado?: KeyboardTypeOptions; secreto?: boolean; maximo?: number; autoFocus?: boolean; multilinea?: boolean;
  contador?: boolean; autoCompletar?: 'username' | 'password' | 'tel' | 'one-time-code' | 'off';
}) {
  const [visible, setVisible] = useState(false);
  return (
    <View style={estilos.campo}>
      <Text style={estilos.etiqueta}>{etiqueta}</Text>
      <View style={[estilos.caja, multilinea && { minHeight: 110, alignItems: 'flex-start' }, error ? estilos.cajaError : null]}>
        <TextInput value={valor} onChangeText={alCambiar} keyboardType={teclado} secureTextEntry={secreto && !visible}
          maxLength={maximo} autoFocus={autoFocus} multiline={multilinea} accessibilityLabel={etiqueta}
          accessibilityHint={ayuda} autoCapitalize="none" autoCorrect={false} autoComplete={autoCompletar}
          style={[estilos.entrada, multilinea && { minHeight: 100, textAlignVertical: 'top', paddingTop: 14 }]}
          placeholderTextColor={color.tintaSuave} />
        {secreto ? (
          <Pressable onPress={() => setVisible((v) => !v)} accessibilityRole="button"
            accessibilityLabel={visible ? 'Ocultar' : 'Mostrar'} accessibilityState={{ selected: visible }} style={estilos.ojo}>
            <Icono nombre={visible ? 'ojoTachado' : 'ojo'} color={color.tinta} />
          </Pressable>
        ) : null}
      </View>
      {error ? (
        <View style={estilos.mensaje}>
          <Icono nombre="alerta" tamano={18} color={color.error} />
          <Text style={[estilos.mensajeTexto, { color: color.error, fontFamily: fuente.negrita }]} accessibilityLiveRegion="polite">{error}</Text>
        </View>
      ) : ayuda ? (
        <View style={estilos.mensaje}>
          <Icono nombre="info" tamano={18} color={color.tintaSuave} />
          <Text style={estilos.mensajeTexto}>{ayuda}</Text>
        </View>
      ) : null}
      {contador && maximo ? <Text style={[estilos.mensajeTexto, { alignSelf: 'flex-end' }]}>{valor.length} de {maximo} caracteres</Text> : null}
    </View>
  );
}

export function Tarjeta({ children, estilo, tono = 'blanco' }: {
  children: ReactNode; estilo?: StyleProp<ViewStyle>; tono?: 'blanco' | 'claro' | 'oscuro';
}) {
  const fondo = { blanco: color.blanco, claro: color.acentoSuave, oscuro: color.tinta }[tono];
  return <View style={[estilos.tarjeta, tono === 'blanco' && sombra, { backgroundColor: fondo }, estilo]}>{children}</View>;
}

export function Texto({ children, tipo = 'cuerpo', claro = false, estilo, lineas }: {
  children: ReactNode; tipo?: 'cuerpo' | 'suave' | 'titulo' | 'subtitulo' | 'grande' | 'pequeno' | 'negrita';
  claro?: boolean; estilo?: StyleProp<TextStyle>; lineas?: number;
}) {
  const base = {
    cuerpo: estilos.cuerpo, suave: estilos.suave, titulo: estilos.titulo, subtitulo: estilos.subtitulo,
    grande: estilos.grande, pequeno: estilos.pequeno, negrita: estilos.negrita,
  }[tipo];
  return (
    <Text style={[base, claro && { color: color.blanco }, estilo]} numberOfLines={lineas}
      accessibilityRole={tipo === 'titulo' || tipo === 'subtitulo' || tipo === 'grande' ? 'header' : undefined}>
      {children}
    </Text>
  );
}

/** Mensaje de error o de éxito con ícono, anunciado por el lector de pantalla. */
export function Aviso({ texto, tipo = 'error', titulo }: { texto: string | null; tipo?: 'error' | 'bien' | 'info'; titulo?: string }) {
  if (!texto) return null;
  const tinte = { error: color.error, bien: color.bien, info: color.acento }[tipo];
  const fondo = { error: color.errorSuave, bien: color.bienSuave, info: color.acentoSuave }[tipo];
  return (
    <View style={[estilos.aviso, { borderColor: tinte, backgroundColor: fondo }]} accessibilityLiveRegion="assertive" accessibilityRole="alert">
      <Icono nombre={tipo === 'bien' ? 'check' : tipo === 'info' ? 'info' : 'alerta'} color={tinte} />
      <View style={{ flex: 1, gap: 2 }}>
        {titulo ? <Text style={[estilos.negrita, { color: tinte }]}>{titulo}</Text> : null}
        <Text style={[estilos.cuerpo, { color: titulo ? color.cuerpo : tinte, fontFamily: titulo ? fuente.cuerpo : fuente.negrita }]}>{texto}</Text>
      </View>
    </View>
  );
}

export function Cargando({ texto = 'Cargando…' }: { texto?: string }) {
  return (
    <View style={estilos.cargando} accessibilityLiveRegion="polite">
      <ActivityIndicator size="large" color={color.acento} />
      <Text style={estilos.suave}>{texto}</Text>
    </View>
  );
}

type Tono = 'info' | 'bien' | 'aviso' | 'error' | 'neutro' | 'marino';

/** Pastilla de estado: el estado va siempre en texto, el color y el ícono solo acompañan. */
export function Etiqueta({ texto, tono = 'info', icono }: { texto: string; tono?: Tono; icono?: NombreIcono }) {
  const fondo = { info: color.acentoSuave, bien: color.bienSuave, aviso: color.avisoSuave, error: color.errorSuave, neutro: color.hundido, marino: color.tinta }[tono];
  const tinta = { info: color.acento, bien: color.bien, aviso: color.aviso, error: color.error, neutro: color.tintaSuave, marino: color.blanco }[tono];
  return (
    <View style={[estilos.etiquetaEstado, { backgroundColor: fondo }]}>
      {icono ? <Icono nombre={icono} tamano={18} color={tinta} /> : null}
      <Text style={{ color: tinta, fontFamily: fuente.negrita, fontSize: letra.pequena }}>{texto}</Text>
    </View>
  );
}

export function Fila({ children, estilo }: { children: ReactNode; estilo?: StyleProp<ViewStyle> }) {
  return <View style={[{ flexDirection: 'row', gap: 10, alignItems: 'center', flexWrap: 'wrap' }, estilo]}>{children}</View>;
}

export function Divisor() {
  return <View style={{ height: 1, backgroundColor: color.bordeSutil }} />;
}

export function PieServicio() {
  return <Text style={[estilos.pequeno, { textAlign: 'center' }]}>Servicio de EPSEL S.A. · Datos referenciales</Text>;
}

export const tecladoNumerico: KeyboardTypeOptions = 'number-pad';

const estilos = StyleSheet.create({
  pantalla: { flex: 1, backgroundColor: color.fondo },
  cabecera: { minHeight: 64, flexDirection: 'row', alignItems: 'center', gap: 12, paddingHorizontal: 12, backgroundColor: color.fondo },
  cabeceraTitulo: { flex: 1, color: color.tinta, fontSize: 22, fontFamily: fuente.titular },
  volver: {
    width: 48, height: 48, borderRadius: medida.radioControl, borderWidth: 1, borderColor: color.bordeSutil,
    backgroundColor: color.blanco, alignItems: 'center', justifyContent: 'center',
  },
  contenido: { padding: medida.margen, paddingTop: 8, gap: 16 },
  pie: { padding: medida.margen, paddingTop: 12, backgroundColor: color.blanco, gap: 10, borderTopWidth: 1, borderTopColor: color.bordeSutil },
  boton: { minHeight: medida.toque, borderRadius: medida.radioControl, paddingHorizontal: 20, alignItems: 'center', justifyContent: 'center' },
  botonBorde: { borderWidth: 2, borderColor: color.acento },
  botonFila: { flexDirection: 'row', alignItems: 'center', gap: 10 },
  botonTexto: { fontSize: letra.entrada, fontFamily: fuente.negrita, textAlign: 'center' },
  campo: { gap: 8 },
  etiqueta: { fontSize: 16, fontFamily: fuente.negrita, color: color.cuerpo },
  caja: {
    minHeight: 52, flexDirection: 'row', alignItems: 'center', borderWidth: 1.5, borderColor: color.borde,
    borderRadius: medida.radioControl, backgroundColor: color.blanco,
  },
  cajaError: { borderColor: color.error, borderWidth: 2.5 },
  entrada: { flex: 1, minHeight: 50, paddingHorizontal: 16, fontSize: letra.entrada, fontFamily: fuente.cuerpo, color: color.cuerpo },
  ojo: { width: 44, height: 44, alignItems: 'center', justifyContent: 'center', marginRight: 4 },
  mensaje: { flexDirection: 'row', gap: 6, alignItems: 'flex-start' },
  mensajeTexto: { flex: 1, fontSize: letra.pequena, fontFamily: fuente.cuerpo, color: color.tintaSuave, lineHeight: 22 },
  tarjeta: { borderRadius: medida.radio, padding: 20, gap: 12 },
  cuerpo: { fontSize: letra.cuerpo, fontFamily: fuente.cuerpo, color: color.cuerpo, lineHeight: 26 },
  negrita: { fontSize: letra.cuerpo, fontFamily: fuente.negrita, color: color.cuerpo, lineHeight: 26 },
  suave: { fontSize: letra.cuerpo, fontFamily: fuente.cuerpo, color: color.tintaSuave, lineHeight: 26 },
  pequeno: { fontSize: letra.pequena, fontFamily: fuente.cuerpo, color: color.tintaSuave, lineHeight: 22 },
  titulo: { fontSize: letra.titulo, fontFamily: fuente.titular, color: color.tinta, lineHeight: 34, letterSpacing: -0.3 },
  subtitulo: { fontSize: 20, fontFamily: fuente.titular, color: color.tinta, lineHeight: 26 },
  grande: { fontSize: letra.grande, fontFamily: fuente.titular, color: color.tinta, lineHeight: 40, letterSpacing: -0.5 },
  aviso: { flexDirection: 'row', gap: 10, borderWidth: 1.5, borderRadius: medida.radioControl, padding: 16 },
  cargando: { padding: 32, alignItems: 'center', gap: 12 },
  etiquetaEstado: {
    alignSelf: 'flex-start', flexDirection: 'row', alignItems: 'center', gap: 6,
    paddingHorizontal: 12, paddingVertical: 6, borderRadius: 999,
  },
});
