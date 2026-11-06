// Piezas de interfaz comunes. Todo lo que se toca mide al menos 48 dp y tiene su etiqueta para el lector
// de pantalla (TalkBack / VoiceOver).
import { type ReactNode } from 'react';
import {
  ActivityIndicator, Pressable, ScrollView, StyleSheet, Text, TextInput, View,
  type KeyboardTypeOptions, type StyleProp, type ViewStyle,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { color, letra, medida } from '../tema';

export function Pantalla({ titulo, alVolver, children, pie }: {
  titulo: string; alVolver?: () => void; children: ReactNode; pie?: ReactNode;
}) {
  const borde = useSafeAreaInsets();
  return (
    <View style={[estilos.pantalla, { paddingTop: borde.top }]}>
      <View style={estilos.cabecera}>
        {alVolver ? (
          <Pressable onPress={alVolver} accessibilityRole="button" accessibilityLabel="Volver"
            style={({ pressed }) => [estilos.volver, pressed && { opacity: 0.6 }]} hitSlop={8}>
            <Text style={estilos.volverTexto}>‹ Volver</Text>
          </Pressable>
        ) : null}
        <Text style={estilos.cabeceraTitulo} accessibilityRole="header" numberOfLines={2}>{titulo}</Text>
      </View>
      <ScrollView contentContainerStyle={[estilos.contenido, { paddingBottom: borde.bottom + 32 }]}
        keyboardShouldPersistTaps="handled">
        {children}
      </ScrollView>
      {pie ? <View style={[estilos.pie, { paddingBottom: borde.bottom + 12 }]}>{pie}</View> : null}
    </View>
  );
}

type Variante = 'principal' | 'secundario' | 'peligro' | 'claro';

export function Boton({ texto, alPresionar, variante = 'principal', cargando = false, deshabilitado = false, estilo, etiqueta }: {
  texto: string; alPresionar: () => void; variante?: Variante; cargando?: boolean; deshabilitado?: boolean;
  estilo?: StyleProp<ViewStyle>; etiqueta?: string;
}) {
  const apagado = deshabilitado || cargando;
  const fondo = { principal: color.acento, secundario: color.blanco, peligro: color.error, claro: color.c4 }[variante];
  const tinta = variante === 'secundario' || variante === 'claro' ? color.tinta : color.blanco;
  return (
    <Pressable onPress={alPresionar} disabled={apagado} accessibilityRole="button"
      accessibilityLabel={etiqueta ?? texto} accessibilityState={{ disabled: apagado, busy: cargando }}
      style={({ pressed }) => [estilos.boton, { backgroundColor: fondo },
        variante === 'secundario' && estilos.botonBorde, apagado && { opacity: 0.55 }, pressed && { opacity: 0.8 }, estilo]}>
      {cargando ? <ActivityIndicator color={tinta} /> : <Text style={[estilos.botonTexto, { color: tinta }]}>{texto}</Text>}
    </Pressable>
  );
}

export function Campo({ etiqueta, valor, alCambiar, ayuda, error, teclado, secreto, maximo, autoFocus, multilinea }: {
  etiqueta: string; valor: string; alCambiar: (t: string) => void; ayuda?: string; error?: string | null;
  teclado?: KeyboardTypeOptions; secreto?: boolean; maximo?: number; autoFocus?: boolean; multilinea?: boolean;
}) {
  return (
    <View style={estilos.campo}>
      <Text style={estilos.etiqueta}>{etiqueta}</Text>
      {ayuda ? <Text style={estilos.ayuda}>{ayuda}</Text> : null}
      <TextInput value={valor} onChangeText={alCambiar} keyboardType={teclado} secureTextEntry={secreto}
        maxLength={maximo} autoFocus={autoFocus} multiline={multilinea} accessibilityLabel={etiqueta}
        accessibilityHint={ayuda} autoCapitalize="none" autoCorrect={false}
        style={[estilos.entrada, multilinea && { minHeight: 96, textAlignVertical: 'top' }, error ? estilos.entradaError : null]}
        placeholderTextColor={color.tintaSuave} />
      {error ? <Text style={estilos.error} accessibilityLiveRegion="polite">{error}</Text> : null}
    </View>
  );
}

export function Tarjeta({ children, estilo, tono = 'blanco' }: {
  children: ReactNode; estilo?: StyleProp<ViewStyle>; tono?: 'blanco' | 'claro' | 'oscuro';
}) {
  const fondo = { blanco: color.blanco, claro: color.c5, oscuro: color.c1 }[tono];
  return <View style={[estilos.tarjeta, { backgroundColor: fondo }, estilo]}>{children}</View>;
}

export function Texto({ children, tipo = 'cuerpo', claro = false, estilo }: {
  children: ReactNode; tipo?: 'cuerpo' | 'suave' | 'titulo' | 'subtitulo' | 'grande' | 'pequeno';
  claro?: boolean; estilo?: StyleProp<import('react-native').TextStyle>;
}) {
  const base = {
    cuerpo: estilos.cuerpo, suave: estilos.suave, titulo: estilos.titulo, subtitulo: estilos.subtitulo,
    grande: estilos.grande, pequeno: estilos.pequeno,
  }[tipo];
  return (
    <Text style={[base, claro && { color: color.blanco }, estilo]}
      accessibilityRole={tipo === 'titulo' || tipo === 'subtitulo' ? 'header' : undefined}>
      {children}
    </Text>
  );
}

/** Mensaje de error o de éxito, anunciado por el lector de pantalla. */
export function Aviso({ texto, tipo = 'error' }: { texto: string | null; tipo?: 'error' | 'bien' | 'info' }) {
  if (!texto) return null;
  const tinte = { error: color.error, bien: color.bien, info: color.acento }[tipo];
  return (
    <View style={[estilos.aviso, { borderColor: tinte }]} accessibilityLiveRegion="assertive" accessibilityRole="alert">
      <Text style={[estilos.cuerpo, { color: tipo === 'error' ? color.error : color.tinta, fontWeight: '700' }]}>{texto}</Text>
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

/** Pastilla de estado: el estado va siempre en texto, el color solo acompaña. */
export function Etiqueta({ texto, tono = 'info' }: { texto: string; tono?: 'info' | 'bien' | 'aviso' | 'error' | 'neutro' }) {
  const fondo = { info: color.c5, bien: '#dcefe3', aviso: '#f6e7cf', error: '#f6dcda', neutro: '#e7ebef' }[tono];
  const tinta = { info: color.c2, bien: color.bien, aviso: color.aviso, error: color.error, neutro: color.tintaSuave }[tono];
  return (
    <View style={[estilos.etiquetaEstado, { backgroundColor: fondo }]}>
      <Text style={{ color: tinta, fontWeight: '800', fontSize: letra.pequena }}>{texto}</Text>
    </View>
  );
}

export function Fila({ children, estilo }: { children: ReactNode; estilo?: StyleProp<ViewStyle> }) {
  return <View style={[{ flexDirection: 'row', gap: 10, alignItems: 'center', flexWrap: 'wrap' }, estilo]}>{children}</View>;
}

export const tecladoNumerico: KeyboardTypeOptions = 'number-pad';

const estilos = StyleSheet.create({
  pantalla: { flex: 1, backgroundColor: color.fondo },
  cabecera: { backgroundColor: color.c1, paddingHorizontal: medida.margen, paddingVertical: 14, gap: 4 },
  cabeceraTitulo: { color: color.blanco, fontSize: letra.entrada, fontWeight: '800' },
  volver: { minHeight: 44, justifyContent: 'center', alignSelf: 'flex-start' },
  volverTexto: { color: color.c4, fontSize: letra.cuerpo, fontWeight: '700' },
  contenido: { padding: medida.margen, gap: 16 },
  pie: { padding: medida.margen, paddingTop: 12, backgroundColor: color.blanco, gap: 10, borderTopWidth: 1, borderTopColor: color.c4 },
  boton: {
    minHeight: medida.toque, borderRadius: 999, borderBottomLeftRadius: 0, paddingHorizontal: 22,
    alignItems: 'center', justifyContent: 'center',
  },
  botonBorde: { borderWidth: 2, borderColor: color.tinta },
  botonTexto: { fontSize: letra.cuerpo, fontWeight: '800', textAlign: 'center' },
  campo: { gap: 6 },
  etiqueta: { fontSize: letra.cuerpo, fontWeight: '800', color: color.tinta },
  ayuda: { fontSize: letra.pequena, color: color.tintaSuave },
  entrada: {
    minHeight: 54, borderWidth: 2, borderColor: color.borde, borderRadius: 18, borderBottomLeftRadius: 0,
    paddingHorizontal: 16, fontSize: letra.entrada, color: color.tinta, backgroundColor: color.blanco,
  },
  entradaError: { borderColor: color.error, borderWidth: 3 },
  error: { color: color.error, fontWeight: '700', fontSize: letra.cuerpo },
  tarjeta: { borderRadius: medida.radio, borderBottomLeftRadius: 0, padding: 18, gap: 10 },
  cuerpo: { fontSize: letra.cuerpo, color: color.tinta, lineHeight: 24 },
  suave: { fontSize: letra.cuerpo, color: color.tintaSuave, lineHeight: 24 },
  pequeno: { fontSize: letra.pequena, color: color.tintaSuave, lineHeight: 21 },
  titulo: { fontSize: letra.titulo, fontWeight: '800', color: color.tinta, letterSpacing: -0.5 },
  subtitulo: { fontSize: letra.entrada, fontWeight: '800', color: color.tinta },
  grande: { fontSize: letra.grande, fontWeight: '800', color: color.tinta, letterSpacing: -1 },
  aviso: { borderWidth: 3, borderRadius: 18, borderBottomLeftRadius: 0, padding: 14, backgroundColor: color.blanco },
  cargando: { padding: 32, alignItems: 'center', gap: 12 },
  etiquetaEstado: { alignSelf: 'flex-start', paddingHorizontal: 12, paddingVertical: 6, borderRadius: 999 },
});
