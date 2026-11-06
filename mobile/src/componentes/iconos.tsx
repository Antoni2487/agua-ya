// Íconos de línea del Figma (24×24, trazo 2). Son decorativos: el texto de al lado dice lo mismo.
import Svg, { Circle, Path } from 'react-native-svg';
import { color as tema } from '../tema';

const TRAZOS = {
  inicio: 'M3 10.5 12 3l9 7.5M5 9v11h5v-6h4v6h5V9',
  mapa: 'M9 4 3 6v14l6-2 6 2 6-2V4l-6 2-6-2Zm0 0v14m6-12v14',
  reportar: 'M5 21V4m0 0h11l-2 4 2 4H5',
  cuenta: 'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm-7 9a7 7 0 0 1 14 0',
  ubicacion: 'M12 21s-7-6.2-7-11.5a7 7 0 0 1 14 0C19 14.8 12 21 12 21Zm0-8.5a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z',
  camion: 'M2 7h12v9H2zM14 10h4l3 3v3h-7M6 19a2 2 0 1 0 0-4 2 2 0 0 0 0 4Zm11 0a2 2 0 1 0 0-4 2 2 0 0 0 0 4Z',
  calendario: 'M4 6h16v14H4zM4 10h16M8 3v4m8-4v4',
  check: 'M20 6 9 17l-5-5',
  escudo: 'M12 3 4 6v6c0 4.5 3.4 8.3 8 9 4.6-.7 8-4.5 8-9V6l-8-3Zm-3.5 9 2.5 2.5 4.5-5',
  atras: 'M15 5 8 12l7 7',
  alerta: 'M12 8v5m0 3.5v.5M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18Z',
  info: 'M12 11v6m0-9.5V8M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18Z',
  salir: 'M15 4h3a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-3M10 8l-4 4 4 4M6 12h10',
  lista: 'M8 6h13M8 12h13M8 18h13M3.5 6h.01M3.5 12h.01M3.5 18h.01',
  candado: 'M6 11h12v10H6zM8 11V8a4 4 0 1 1 8 0v3',
  cruz: 'M12 5v14M5 12h14',
} as const;

export type NombreIcono = keyof typeof TRAZOS | 'estrella' | 'estrellaVacia' | 'ojo' | 'ojoTachado';

export function Icono({ nombre, tamano = 24, color = tema.tinta }: { nombre: NombreIcono; tamano?: number; color?: string }) {
  const comun = { width: tamano, height: tamano, viewBox: '0 0 24 24', accessibilityElementsHidden: true, importantForAccessibility: 'no-hide-descendants' as const };
  if (nombre === 'estrella' || nombre === 'estrellaVacia') {
    return (
      <Svg {...comun}>
        <Path d="m12 3 2.8 5.7 6.2.9-4.5 4.4 1 6.2L12 17.3 6.5 20.2l1-6.2L3 9.6l6.2-.9L12 3Z"
          fill={nombre === 'estrella' ? color : 'none'} stroke={color} strokeWidth={1.6} strokeLinejoin="round" />
      </Svg>
    );
  }
  if (nombre === 'ojo' || nombre === 'ojoTachado') {
    return (
      <Svg {...comun} fill="none" stroke={color} strokeWidth={2} strokeLinecap="round" strokeLinejoin="round">
        <Path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z" />
        <Circle cx={12} cy={12} r={3} />
        {nombre === 'ojoTachado' ? <Path d="M4 4l16 16" /> : null}
      </Svg>
    );
  }
  return (
    <Svg {...comun} fill="none" stroke={color} strokeWidth={2} strokeLinecap="round" strokeLinejoin="round">
      <Path d={TRAZOS[nombre]} />
    </Svg>
  );
}

/** Isotipo de AguaYa: gota con el camión cisterna (como en el Figma). */
export function Isotipo({ tamano = 36 }: { tamano?: number }) {
  return (
    <Svg width={tamano} height={tamano * 1.2} viewBox="0 0 60 72" accessibilityElementsHidden importantForAccessibility="no-hide-descendants">
      <Path d="M30 2C30 2 4 32 4 46a26 26 0 0 0 52 0C56 32 30 2 30 2Z" fill={tema.agua} />
      <Path d="M30 2C30 2 4 32 4 46a26 26 0 0 0 26 26V2Z" fill="#2aa0e0" opacity={0.6} />
      <Path d="M17 44h16v10H17zM33 47h6l4 4v3H33z" fill="#ffffff" />
      <Circle cx={22} cy={56} r={3} fill={tema.tinta} />
      <Circle cx={38} cy={56} r={3} fill={tema.tinta} />
      <Path d="M24 30a9 9 0 0 1 12 0M21 26a14 14 0 0 1 18 0" stroke="#ffffff" strokeWidth={2.4} fill="none" strokeLinecap="round" />
    </Svg>
  );
}
