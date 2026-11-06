// Reporte de incidencia (RF06, Figma 04) para el vecino y el conductor. Si falta elegir qué pasó, se muestra un
// resumen del error arriba (como en el Figma) además del mensaje junto a las opciones. El admin recibe la alerta al instante.
import { useState, type ReactNode } from 'react';
import { Pressable, StyleSheet, Switch, Text, View } from 'react-native';
import { api, textoError } from '../api/cliente';
import type { Atencion, TipoIncidencia } from '../api/tipos';
import { Icono } from '../componentes/iconos';
import { Aviso, Boton, Campo, Pantalla, Texto } from '../componentes/ui';
import { color, fuente, medida } from '../tema';
import { cuando, TURNO_CORTO } from '../textos';

type Opcion = { tipo: TipoIncidencia; titulo: string; detalle: string };

const OPCIONES: Record<'VECINO' | 'CONDUCTOR', Opcion[]> = {
  VECINO: [
    { tipo: 'NO_LLEGO', titulo: 'No llegó', detalle: 'La cisterna no vino a mi parada' },
    { tipo: 'LLEGO_TARDE', titulo: 'Llegó tarde', detalle: 'Vino mucho después de lo esperado' },
    { tipo: 'YA_PASO', titulo: 'Ya pasó y no paró', detalle: 'La vi pasar pero no me atendió' },
    { tipo: 'VOLUMEN_INSUFICIENTE', titulo: 'Dejó poca agua', detalle: 'No alcanzó para mi casa' },
    { tipo: 'OTRO', titulo: 'Otro problema', detalle: 'Cuéntanos abajo' },
  ],
  CONDUCTOR: [
    { tipo: 'AVERIA', titulo: 'Avería del camión', detalle: 'El camión tiene una falla' },
    { tipo: 'ACCESO_BLOQUEADO', titulo: 'Acceso bloqueado', detalle: 'No se puede llegar a la parada' },
    { tipo: 'AGUA_AGOTADA', titulo: 'Se acabó el agua', detalle: 'Hay que volver a cargar' },
    { tipo: 'OTRO', titulo: 'Otro problema', detalle: 'Cuéntalo abajo' },
  ],
};

export function Reportar({ rol, rutaId, paradaId, atencion, sector, barra, alVolver, alTerminar, alVerReportes }: {
  rol: 'CONDUCTOR' | 'VECINO'; rutaId?: number; paradaId?: number; atencion?: Atencion | null; sector?: string;
  barra?: ReactNode; alVolver: () => void; alTerminar?: () => void; alVerReportes?: () => void;
}) {
  const ruta = rutaId ?? atencion?.rutaId;
  const parada = paradaId ?? atencion?.miParada?.paradaId;
  const [tipo, setTipo] = useState<TipoIncidencia | null>(null);
  const [descripcion, setDescripcion] = useState('');
  const [detiene, setDetiene] = useState(false);
  const [faltaTipo, setFaltaTipo] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [enviado, setEnviado] = useState(false);
  const [enviando, setEnviando] = useState(false);

  const enviar = async () => {
    setError(null);
    if (!tipo) { setFaltaTipo(true); return; }
    if (tipo === 'OTRO' && !descripcion.trim()) { setError('Cuéntanos qué pasó en el cuadro de abajo.'); return; }
    setEnviando(true);
    try {
      await api('/api/incidencias', {
        metodo: 'POST',
        cuerpo: { rutaId: ruta, paradaId: parada ?? null, tipo, descripcion: descripcion.trim() || null, detieneRuta: rol === 'CONDUCTOR' ? detiene : null },
      });
      if (alTerminar) { alTerminar(); return; }
      setEnviado(true); setTipo(null); setDescripcion('');
    } catch (e) {
      setError(textoError(e));
    } finally {
      setEnviando(false);
    }
  };

  if (!ruta) {
    return (
      <Pantalla titulo="Reportar un problema" alVolver={alVolver} barra={barra}>
        <Texto tipo="subtitulo">Aún no hay una ruta para reportar</Texto>
        <Texto>Podrás reportar un problema cuando EPSEL programe la cisterna para tu sector.</Texto>
        {alVerReportes ? <Boton texto="Ver mis reportes" variante="secundario" icono="lista" alPresionar={alVerReportes} /> : null}
      </Pantalla>
    );
  }

  return (
    <Pantalla titulo="Reportar un problema" alVolver={alVolver} barra={barra}>
      {faltaTipo && !tipo ? <Aviso titulo="No pudimos enviar tu reporte" texto="Elige qué pasó con la cisterna (1 error)." /> : null}
      {enviado ? <Aviso tipo="bien" titulo="Reporte enviado" texto="EPSEL ya lo recibió. Puedes seguirlo en Mis reportes." /> : null}
      {atencion ? (
        <Texto tipo="pequeno">Ruta de {cuando(atencion.fecha).toLowerCase()} · {sector ?? 'tu sector'} · {TURNO_CORTO[atencion.turno]}</Texto>
      ) : null}

      <View accessibilityRole="radiogroup" accessibilityLabel="¿Qué pasó?" style={{ gap: 10 }}>
        <Texto tipo="subtitulo">¿Qué pasó?</Texto>
        {faltaTipo && !tipo ? (
          <View style={estilos.falta}>
            <Icono nombre="alerta" tamano={18} color={color.error} />
            <Text style={estilos.faltaTexto}>Elige una opción para continuar.</Text>
          </View>
        ) : null}
        {OPCIONES[rol].map((o) => {
          const sel = tipo === o.tipo;
          return (
            <Pressable key={o.tipo} onPress={() => { setTipo(o.tipo); setEnviado(false); }} accessibilityRole="radio"
              accessibilityState={{ checked: sel }} accessibilityLabel={`${o.titulo}. ${o.detalle}`}
              style={[estilos.opcion, sel && estilos.opcionActiva, faltaTipo && !tipo && { borderColor: color.error }]}>
              <View style={[estilos.radio, sel && estilos.radioActivo]}>{sel ? <View style={estilos.radioPunto} /> : null}</View>
              <View style={{ flex: 1, gap: 2 }}>
                <Text style={estilos.opcionTitulo}>{o.titulo}</Text>
                <Text style={estilos.opcionDetalle}>{o.detalle}</Text>
              </View>
            </Pressable>
          );
        })}
      </View>

      <Campo etiqueta="Cuéntanos más (opcional)" valor={descripcion} alCambiar={setDescripcion} maximo={500} multilinea contador />

      {rol === 'CONDUCTOR' ? (
        <View style={estilos.interruptor}>
          <View style={{ flex: 1, gap: 4 }}>
            <Texto tipo="negrita">¿No puedes seguir la ruta?</Texto>
            <Texto tipo="pequeno">Si lo activas, la ruta queda interrumpida y EPSEL reasigna las paradas que faltan.</Texto>
          </View>
          <Switch value={detiene} onValueChange={setDetiene} accessibilityLabel="La ruta no puede seguir"
            trackColor={{ true: color.error, false: color.c4 }} thumbColor={color.blanco} />
        </View>
      ) : null}
      <Aviso texto={error} />
      <Boton texto="Enviar reporte" alPresionar={enviar} cargando={enviando} variante={detiene ? 'peligro' : 'principal'} />
      {alVerReportes ? <Boton texto="Ver mis reportes" variante="secundario" icono="lista" alPresionar={alVerReportes} /> : null}
    </Pantalla>
  );
}

const estilos = StyleSheet.create({
  falta: { flexDirection: 'row', gap: 6, alignItems: 'center' },
  faltaTexto: { fontSize: 15, fontFamily: fuente.negrita, color: color.error },
  opcion: {
    minHeight: 72, flexDirection: 'row', alignItems: 'center', gap: 14, paddingHorizontal: 18, paddingVertical: 12,
    borderWidth: 1.5, borderColor: color.bordeSutil, borderRadius: medida.radioControl + 2, backgroundColor: color.blanco,
  },
  opcionActiva: { borderColor: color.acento, borderWidth: 2.5, backgroundColor: color.acentoSuave },
  opcionTitulo: { fontSize: 18, fontFamily: fuente.negrita, color: color.cuerpo },
  opcionDetalle: { fontSize: 15, fontFamily: fuente.cuerpo, color: color.tintaSuave },
  radio: { width: 24, height: 24, borderRadius: 12, borderWidth: 2, borderColor: color.borde, alignItems: 'center', justifyContent: 'center' },
  radioActivo: { borderColor: color.acento },
  radioPunto: { width: 12, height: 12, borderRadius: 6, backgroundColor: color.acento },
  interruptor: { flexDirection: 'row', alignItems: 'center', gap: 12, padding: 16, backgroundColor: color.blanco, borderRadius: medida.radioControl },
});
