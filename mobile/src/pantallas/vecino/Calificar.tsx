import { useState } from 'react';
import { Pressable, StyleSheet, Text } from 'react-native';
import { api, textoError } from '../../api/cliente';
import { Aviso, Boton, Campo, Fila, Pantalla, Texto } from '../../componentes/ui';
import { color } from '../../tema';

const SIGNIFICADO = ['', 'Muy mal', 'Mal', 'Regular', 'Bien', 'Muy bien'];

export function Calificar({ paradaId, alVolver, alTerminar }: { paradaId: number; alVolver: () => void; alTerminar: () => void }) {
  const [estrellas, setEstrellas] = useState(0);
  const [comentario, setComentario] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  const enviar = async () => {
    setError(null);
    if (estrellas === 0) { setError('Elige de 1 a 5 estrellas.'); return; }
    setEnviando(true);
    try {
      await api('/api/vecino/calificaciones', { metodo: 'POST', cuerpo: { paradaId, estrellas, comentario: comentario.trim() || null } });
      alTerminar();
    } catch (e) {
      setError(textoError(e));
    } finally {
      setEnviando(false);
    }
  };

  return (
    <Pantalla titulo="Calificar la entrega" alVolver={alVolver}
      pie={<Boton texto="Enviar calificación" alPresionar={enviar} cargando={enviando} />}>
      <Texto tipo="subtitulo">¿Cómo te atendió la cisterna?</Texto>
      <Fila estilo={{ gap: 6 }}>
        {[1, 2, 3, 4, 5].map((n) => (
          <Pressable key={n} onPress={() => setEstrellas(n)} accessibilityRole="radio"
            accessibilityState={{ selected: estrellas === n }} accessibilityLabel={`${n} de 5 estrellas: ${SIGNIFICADO[n]}`}
            style={[estilos.estrella, n <= estrellas && estilos.estrellaActiva]}>
            <Text style={[estilos.estrellaTexto, n <= estrellas && { color: color.blanco }]}>★</Text>
          </Pressable>
        ))}
      </Fila>
      <Texto>{estrellas ? `${estrellas} de 5: ${SIGNIFICADO[estrellas]}` : 'Toca una estrella.'}</Texto>
      <Campo etiqueta="Comentario (opcional)" valor={comentario} alCambiar={setComentario} maximo={500} multilinea />
      <Aviso texto={error} />
    </Pantalla>
  );
}

const estilos = StyleSheet.create({
  estrella: {
    width: 56, height: 56, borderRadius: 28, borderBottomLeftRadius: 0, borderWidth: 2, borderColor: color.tinta,
    alignItems: 'center', justifyContent: 'center', backgroundColor: color.blanco,
  },
  estrellaActiva: { backgroundColor: color.acento, borderColor: color.acento },
  estrellaTexto: { fontSize: 26, color: color.tinta },
});
