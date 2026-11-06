// Tokens del diseño del vecino en Figma (archivo "AguaYa", página Fundamentos). Contraste ≥ 4.5:1 sobre arena y blanco.
export const color = {
  // Fondos
  fondo: '#f6f3ee',        // fondo/arena
  blanco: '#ffffff',       // fondo/superficie
  hundido: '#eee9e1',      // fondo/hundido (segmentos por venir, mapa)
  // Texto
  tinta: '#0e2a5c',        // texto/titulo y marca/marino
  cuerpo: '#1c2433',       // texto/cuerpo
  tintaSuave: '#4e5a6e',   // texto/suave
  // Acción y marca
  acento: '#0a5bb5',       // accion/base
  acentoSuave: '#e6f0fa',  // accion/suave
  agua: '#1b8fd2',         // marca/agua
  tuCasa: '#c2410c',       // estado/tu-casa (pin y segmento de tu parada)
  // Estados (siempre acompañados de texto)
  bien: '#1f7a4d',
  bienSuave: '#e3f4ea',
  error: '#b3261e',
  errorSuave: '#fdecea',
  aviso: '#8a5200',
  avisoSuave: '#fff1d6',
  // Bordes
  bordeSutil: '#e2dcd2',
  borde: '#6b7588',        // borde/control
  // Alias que usan las pantallas del conductor
  c1: '#0e2a5c',
  c2: '#13406e',
  c3: '#0a5bb5',
  c4: '#a9d2f0',
  c5: '#e6f0fa',
};

export const medida = {
  margen: 16,
  toque: 56,      // alto de botones del Figma (≥ 48 dp)
  radio: 20,      // tarjetas
  radioControl: 12,
};

export const letra = {
  pequena: 15,
  cuerpo: 17,
  entrada: 18,
  titulo: 28,
  grande: 34,
};

// Titulares en Nunito; cuerpo y controles en Atkinson Hyperlegible (pensada para baja visión).
export const fuente = {
  titular: 'Nunito_800ExtraBold',
  titularMedio: 'Nunito_700Bold',
  cuerpo: 'AtkinsonHyperlegible_400Regular',
  negrita: 'AtkinsonHyperlegible_700Bold',
};

export const sombra = {
  shadowColor: '#0e2a5c',
  shadowOpacity: 0.08,
  shadowRadius: 12,
  shadowOffset: { width: 0, height: 4 },
  elevation: 3,
};
