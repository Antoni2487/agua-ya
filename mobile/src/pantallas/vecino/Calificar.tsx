// Calificar la entrega (RF08, Figma 05): contexto de la entrega, 1 a 5 estrellas grandes y un comentario.
import { useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { api, textoError } from '../../api/cliente';
import type { Atencion } from '../../api/tipos';
import { Icono } from '../../componentes/iconos';
import { Aviso, Boton, Campo, Fila, Pantalla, Tarjeta, Texto } from '../../componentes/ui';
import { PlacaCisterna } from '../../componentes/vecino';
import { color, fuente } from '../../tema';
import { cuando, TURNO_CORTO } from '../../textos';

const SIGNIFICADO = ['', 'Muy mala', 'Mala', 'Regular', 'Buena', 'Muy buena'];

export function Calificar({ atencion, alVolver, alTerminar }: { atencion: Atencion; alVolver: () => void; alTerminar: () => void }) {
  const [estrellas, setEstrellas] = useState(0);
  const [comentario, setComentario] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);
  const mi = atencion.miParada;

  const enviar = async () => {
    setError(null);
    if (!mi) return;
    if (estrellas === 0) { setError('Elige de 1 a 5 estrellas.'); return; }
    setEnviando(true);
    try {
      await api('/api/vecino/calificaciones', { metodo: 'POST', cuerpo: { paradaId: mi.paradaId, estrellas, comentario: comentario.trim() || null } });
      alTerminar();
    } catch (e) {
      setError(textoError(e));
    } finally {
      setEnviando(false);
    }
  };

  return (
    <Pantalla titulo="Califica la entrega" alVolver={alVolver}>
      <Tarjeta>
        <Texto tipo="negrita">{cuando(atencion.fecha)} · {TURNO_CORTO[atencion.turno]}</Texto>
        {atencion.cisternaPlaca ? <PlacaCisterna placa={atencion.cisternaPlaca} capacidad={atencion.capacidadLitros ?? null} /> : null}
        {mi?.verificadaGps ? (
          <Fila estilo={{ gap: 6 }}>
            <Icono nombre="escudo" tamano={20} color={color.bien} />
            <Text style={{ fontFamily: fuente.cuerpo, fontSize: 15, color: color.bien }}>Entrega verificada por GPS</Text>
          </Fila>
        ) : null}
      </Tarjeta>

      <View accessibilityRole="radiogroup" accessibilityLabel="¿Cómo fue la entrega?" style={{ gap: 12 }}>
        <Texto tipo="subtitulo">¿Cómo fue la entrega?</Texto>
        <View style={estilos.estrellas}>
          {[1, 2, 3, 4, 5].map((n) => {
            const llena = n <= estrellas;
            return (
              <Pressable key={n} onPress={() => setEstrellas(n)} accessibilityRole="radio"
                accessibilityState={{ checked: estrellas === n }} accessibilityLabel={`${n} de 5: ${SIGNIFICADO[n]}`}
                style={[estilos.estrella, estrellas === n && estilos.estrellaElegida]}>
                <Icono nombre={llena ? 'estrella' : 'estrellaVacia'} tamano={36} color={llena ? color.acento : color.borde} />
                <Text style={estilos.numero}>{n}</Text>
              </Pressable>
            );
          })}
        </View>
        <Texto tipo="negrita">{estrellas ? `${estrellas} de 5: ${SIGNIFICADO[estrellas]}` : 'Toca una estrella.'}</Texto>
      </View>

      <Campo etiqueta="Comentario (opcional)" valor={comentario} alCambiar={setComentario} maximo={500} multilinea contador />
      <Aviso texto={error} />
      <Boton texto="Enviar calificación" alPresionar={enviar} cargando={enviando} />
    </Pantalla>
  );
}

const estilos = StyleSheet.create({
  estrellas: { flexDirection: 'row', justifyContent: 'space-between' },
  estrella: { width: 58, minHeight: 76, alignItems: 'center', justifyContent: 'center', borderRadius: 12, gap: 2 },
  estrellaElegida: { backgroundColor: color.acentoSuave },
  numero: { fontSize: 15, fontFamily: fuente.negrita, color: color.cuerpo },
});
