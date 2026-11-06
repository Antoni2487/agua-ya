// Piezas propias de las pantallas del vecino (Figma "Vecino · móvil"): cabecera con la marca, gota de avance,
// tira de paradas, placa de la cisterna y barra de pestañas.
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import Svg, { ClipPath, Defs, Path, Rect } from 'react-native-svg';
import { color, fuente, letra } from '../tema';
import { Icono, Isotipo, type NombreIcono } from './iconos';

export function CabeceraVecino() {
  return (
    <View style={estilos.cabecera} accessibilityRole="header" accessibilityLabel="AguaYa">
      <Isotipo tamano={28} />
      <Text style={estilos.marca}>Agua<Text style={{ color: color.acento }}>Ya</Text></Text>
    </View>
  );
}

/** Gota que se llena según el avance de la ruta (atendidas ÷ total). */
export function GotaAvance({ avance }: { avance: number }) {
  const nivel = Math.max(0, Math.min(1, avance));
  const alto = 100;
  const y = 8 + (1 - nivel) * (alto - 8);
  const gota = 'M40 2C40 2 6 44 6 64a34 34 0 0 0 68 0C74 44 40 2 40 2Z';
  return (
    <Svg width={80} height={100} viewBox="0 0 80 100" accessibilityElementsHidden importantForAccessibility="no-hide-descendants">
      <Defs><ClipPath id="gota"><Path d={gota} /></ClipPath></Defs>
      <Path d={gota} fill={color.acentoSuave} />
      <Rect x={0} y={y} width={80} height={alto} fill={color.agua} clipPath="url(#gota)" />
      <Rect x={0} y={y + 12} width={80} height={alto} fill={color.acento} opacity={0.55} clipPath="url(#gota)" />
      <Path d={gota} fill="none" stroke={color.tinta} strokeWidth={2.5} />
    </Svg>
  );
}

/**
 * Una barrita por parada, en el orden de la ruta: llenas las atendidas, punteadas las que faltan antes de la tuya,
 * la tuya más alta y naranja, y en gris las que vienen después. El vecino no ve las paradas de otros: solo cuántas.
 */
export function TiraParadas({ total, orden, pendientesAntes, enCurso }: {
  total: number; orden: number | null; pendientesAntes: number; enCurso: boolean;
}) {
  const atendidasAntes = orden ? orden - 1 - pendientesAntes : 0;
  const segmentos = Array.from({ length: total }, (_, i) => {
    const n = i + 1;
    if (orden && n === orden) return 'tuya';
    if (enCurso && orden && n < orden) return n <= atendidasAntes ? 'atendida' : 'antes';
    return 'despues';
  });
  return (
    <View style={estilos.tira} accessibilityElementsHidden importantForAccessibility="no-hide-descendants">
      {segmentos.map((s, i) => <View key={i} style={[estilos.segmento, estilos[s]]} />)}
    </View>
  );
}

export function Muestra({ tipo }: { tipo: 'atendida' | 'antes' | 'tuya' | 'despues' }) {
  return <View style={[estilos.segmento, estilos[tipo], { flex: 0, width: 10, height: tipo === 'tuya' ? 16 : 14 }]} />;
}

export function PlacaCisterna({ placa, capacidad }: { placa: string; capacidad: number | null }) {
  return (
    <View style={estilos.placa} accessibilityLabel={`Placa de la cisterna ${placa.split('').join(' ')}${capacidad ? `, de ${capacidad} litros` : ''}`}>
      <Text style={estilos.placaTexto}>{placa}</Text>
      {capacidad ? (
        <>
          <View style={estilos.placaDivisor} />
          <Text style={estilos.placaDetalle}>Cisterna de {new Intl.NumberFormat('es-PE').format(capacidad)} L</Text>
        </>
      ) : null}
    </View>
  );
}

export type PestanaVecino = 'inicio' | 'mapa' | 'reportar' | 'cuenta';

const PESTANAS: { id: PestanaVecino; texto: string; icono: NombreIcono }[] = [
  { id: 'inicio', texto: 'Inicio', icono: 'inicio' },
  { id: 'mapa', texto: 'Mapa', icono: 'mapa' },
  { id: 'reportar', texto: 'Reportar', icono: 'reportar' },
  { id: 'cuenta', texto: 'Mi cuenta', icono: 'cuenta' },
];

export function BarraVecino({ activa, alElegir }: { activa: PestanaVecino; alElegir: (p: PestanaVecino) => void }) {
  const borde = useSafeAreaInsets();
  return (
    <View style={[estilos.barra, { paddingBottom: borde.bottom + 8 }]} accessibilityRole="tablist">
      {PESTANAS.map((p) => {
        const sel = p.id === activa;
        return (
          <Pressable key={p.id} onPress={() => alElegir(p.id)} accessibilityRole="tab" accessibilityState={{ selected: sel }}
            accessibilityLabel={p.texto} style={[estilos.pestana, sel && estilos.pestanaActiva]}>
            <Icono nombre={p.icono} color={sel ? color.acento : color.tintaSuave} />
            <Text style={[estilos.pestanaTexto, sel && { color: color.acento, fontFamily: fuente.negrita }]}>{p.texto}</Text>
          </Pressable>
        );
      })}
    </View>
  );
}

const estilos = StyleSheet.create({
  cabecera: { minHeight: 64, flexDirection: 'row', alignItems: 'center', gap: 8, paddingHorizontal: 16 },
  marca: { fontSize: 24, fontFamily: fuente.titular, color: color.tinta },
  tira: { flexDirection: 'row', alignItems: 'flex-end', gap: 3, height: 30 },
  segmento: { flex: 1, height: 22, borderRadius: 4, minWidth: 3 },
  atendida: { backgroundColor: color.acento },
  antes: { borderWidth: 1.5, borderColor: color.tinta, borderStyle: 'dashed', backgroundColor: color.blanco },
  tuya: { height: 30, backgroundColor: color.blanco, borderWidth: 3, borderColor: color.tuCasa, flex: 1.4 },
  despues: { backgroundColor: color.hundido, borderWidth: 1, borderColor: color.bordeSutil },
  placa: {
    flexDirection: 'row', alignItems: 'center', gap: 12, alignSelf: 'flex-start', paddingHorizontal: 16, minHeight: 52,
    borderWidth: 2, borderColor: color.tinta, borderRadius: 10, backgroundColor: color.blanco,
  },
  placaTexto: { fontSize: 24, fontFamily: fuente.titular, color: color.tinta, letterSpacing: 2 },
  placaDivisor: { width: 1, height: 28, backgroundColor: color.bordeSutil },
  placaDetalle: { fontSize: letra.pequena, fontFamily: fuente.cuerpo, color: color.tintaSuave },
  barra: {
    flexDirection: 'row', gap: 4, paddingHorizontal: 8, paddingTop: 8, backgroundColor: color.blanco,
    borderTopWidth: 1, borderTopColor: color.bordeSutil,
  },
  pestana: { flex: 1, minHeight: 60, alignItems: 'center', justifyContent: 'center', gap: 4, borderRadius: 14 },
  pestanaActiva: { backgroundColor: color.acentoSuave },
  pestanaTexto: { fontSize: 14, fontFamily: fuente.cuerpo, color: color.tintaSuave },
});
