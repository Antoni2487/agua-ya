// Reporte de incidencia (RF06), para el conductor y el vecino. El admin recibe la alerta al instante.
import { useState } from 'react';
import { Pressable, StyleSheet, Switch, Text, View } from 'react-native';
import { api, textoError } from '../api/cliente';
import type { TipoIncidencia } from '../api/tipos';
import { Aviso, Boton, Campo, Pantalla, Texto } from '../componentes/ui';
import { color, letra } from '../tema';
import { INCIDENCIAS_CONDUCTOR, INCIDENCIAS_VECINO } from '../textos';

export function Reportar({ rol, rutaId, paradaId, alVolver, alTerminar }: {
  rol: 'CONDUCTOR' | 'VECINO'; rutaId: number; paradaId?: number; alVolver: () => void; alTerminar: () => void;
}) {
  const opciones = rol === 'CONDUCTOR' ? INCIDENCIAS_CONDUCTOR : INCIDENCIAS_VECINO;
  const [tipo, setTipo] = useState<TipoIncidencia | null>(null);
  const [descripcion, setDescripcion] = useState('');
  const [detiene, setDetiene] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  const enviar = async () => {
    setError(null);
    if (!tipo) { setError('Elige qué pasó.'); return; }
    setEnviando(true);
    try {
      await api('/api/incidencias', {
        metodo: 'POST',
        cuerpo: { rutaId, paradaId: paradaId ?? null, tipo, descripcion: descripcion.trim() || null, detieneRuta: rol === 'CONDUCTOR' ? detiene : null },
      });
      alTerminar();
    } catch (e) {
      setError(textoError(e));
    } finally {
      setEnviando(false);
    }
  };

  return (
    <Pantalla titulo="Reportar un problema" alVolver={alVolver}
      pie={<Boton texto="Enviar reporte" alPresionar={enviar} cargando={enviando} variante={detiene ? 'peligro' : 'principal'} />}>
      <Texto tipo="subtitulo">¿Qué pasó?</Texto>
      <View accessibilityRole="radiogroup" style={{ gap: 8 }}>
        {opciones.map((o) => (
          <Pressable key={o.tipo} onPress={() => setTipo(o.tipo)} accessibilityRole="radio"
            accessibilityState={{ selected: tipo === o.tipo }}
            style={[estilos.opcion, tipo === o.tipo && estilos.opcionActiva]}>
            <View style={[estilos.punto, tipo === o.tipo && estilos.puntoActivo]} />
            <Text style={[estilos.opcionTexto, tipo === o.tipo && { color: color.blanco }]}>{o.texto}</Text>
          </Pressable>
        ))}
      </View>
      <Campo etiqueta="Cuéntanos más (opcional)" valor={descripcion} alCambiar={setDescripcion} maximo={500} multilinea />
      {rol === 'CONDUCTOR' ? (
        <View style={estilos.interruptor}>
          <View style={{ flex: 1, gap: 4 }}>
            <Texto tipo="subtitulo">¿No puedes seguir la ruta?</Texto>
            <Texto tipo="pequeno">Si lo activas, la ruta queda interrumpida y EPSEL reasigna las paradas que faltan.</Texto>
          </View>
          <Switch value={detiene} onValueChange={setDetiene} accessibilityLabel="La ruta no puede seguir"
            trackColor={{ true: color.error, false: color.c4 }} thumbColor={color.blanco} />
        </View>
      ) : null}
      <Aviso texto={error} />
    </Pantalla>
  );
}

const estilos = StyleSheet.create({
  opcion: {
    minHeight: 56, flexDirection: 'row', alignItems: 'center', gap: 12, paddingHorizontal: 16,
    borderWidth: 2, borderColor: color.tinta, borderRadius: 22, borderBottomLeftRadius: 0, backgroundColor: color.blanco,
  },
  opcionActiva: { backgroundColor: color.c2, borderColor: color.c2 },
  opcionTexto: { fontSize: letra.cuerpo, fontWeight: '700', color: color.tinta, flex: 1 },
  punto: { width: 20, height: 20, borderRadius: 10, borderWidth: 2, borderColor: color.tinta },
  puntoActivo: { backgroundColor: color.blanco, borderColor: color.blanco },
  interruptor: { flexDirection: 'row', alignItems: 'center', gap: 12, padding: 16, backgroundColor: color.blanco, borderRadius: 22, borderBottomLeftRadius: 0 },
});
