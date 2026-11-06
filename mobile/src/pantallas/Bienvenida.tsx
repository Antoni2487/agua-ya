import { StyleSheet, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Boton, Texto } from '../componentes/ui';
import { color } from '../tema';

export function Bienvenida({ alElegir }: { alElegir: (rol: 'vecino' | 'conductor') => void }) {
  const borde = useSafeAreaInsets();
  return (
    <View style={[estilos.fondo, { paddingTop: borde.top + 48, paddingBottom: borde.bottom + 24 }]}>
      <View style={estilos.marca}>
        <View style={estilos.gota} accessibilityElementsHidden importantForAccessibility="no-hide-descendants" />
        <Texto tipo="grande" claro>AguaYa</Texto>
        <Texto claro estilo={estilos.bajada}>La cisterna de EPSEL, a tiempo y a la vista.</Texto>
      </View>
      <View style={estilos.botones}>
        <Texto claro tipo="subtitulo">¿Cómo vas a usar la app?</Texto>
        <Boton texto="Soy vecino: quiero saber cuándo llega el agua" variante="claro" alPresionar={() => alElegir('vecino')} />
        <Boton texto="Soy conductor de cisterna" variante="secundario" alPresionar={() => alElegir('conductor')} />
        <Texto tipo="pequeno" claro estilo={{ opacity: 0.9 }}>
          Sistema de abastecimiento con cisternas de EPSEL S.A. · Proyecto académico UTP.
        </Texto>
      </View>
    </View>
  );
}

const estilos = StyleSheet.create({
  fondo: { flex: 1, backgroundColor: color.c1, paddingHorizontal: 20, justifyContent: 'space-between' },
  marca: { gap: 10 },
  gota: {
    width: 64, height: 64, backgroundColor: color.c4, borderTopLeftRadius: 32, borderTopRightRadius: 32,
    borderBottomRightRadius: 32, borderBottomLeftRadius: 0, transform: [{ rotate: '-45deg' }], marginBottom: 12,
  },
  bajada: { fontSize: 20, lineHeight: 28 },
  botones: { gap: 14 },
});
