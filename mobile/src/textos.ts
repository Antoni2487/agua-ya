// Textos de estados y fechas, siempre en palabras del vecino y del conductor.
import type { EstadoParada, EstadoRuta, TipoIncidencia, Turno } from './api/tipos';

export const TURNO: Record<Turno, string> = { MANANA: 'Turno mañana', TARDE: 'Turno tarde' };

export const ESTADO_RUTA: Record<EstadoRuta, { texto: string; tono: 'info' | 'bien' | 'aviso' | 'error' | 'neutro' }> = {
  PROGRAMADA: { texto: 'Programada', tono: 'info' },
  EN_CURSO: { texto: 'En camino', tono: 'bien' },
  INTERRUMPIDA: { texto: 'Interrumpida', tono: 'aviso' },
  COMPLETADA: { texto: 'Completada', tono: 'neutro' },
};

export const ESTADO_PARADA: Record<EstadoParada, { texto: string; tono: 'info' | 'bien' | 'aviso' | 'error' | 'neutro' }> = {
  PENDIENTE: { texto: 'Pendiente', tono: 'info' },
  ATENDIDA: { texto: 'Atendida', tono: 'bien' },
  NO_ATENDIDA: { texto: 'No atendida', tono: 'error' },
  REASIGNADA: { texto: 'Pasó a otra ruta', tono: 'neutro' },
};

export const INCIDENCIAS_CONDUCTOR: { tipo: TipoIncidencia; texto: string }[] = [
  { tipo: 'AVERIA', texto: 'Avería del camión' },
  { tipo: 'ACCESO_BLOQUEADO', texto: 'Acceso bloqueado' },
  { tipo: 'AGUA_AGOTADA', texto: 'Se acabó el agua' },
  { tipo: 'OTRO', texto: 'Otro problema' },
];

export const INCIDENCIAS_VECINO: { tipo: TipoIncidencia; texto: string }[] = [
  { tipo: 'NO_LLEGO', texto: 'La cisterna no llegó' },
  { tipo: 'LLEGO_TARDE', texto: 'Llegó tarde' },
  { tipo: 'YA_PASO', texto: 'Pasó y no me atendió' },
  { tipo: 'VOLUMEN_INSUFICIENTE', texto: 'Me dieron poca agua' },
  { tipo: 'OTRO', texto: 'Otro problema' },
];

export const NOMBRE_INCIDENCIA: Record<TipoIncidencia, string> = Object.fromEntries(
  [...INCIDENCIAS_CONDUCTOR, ...INCIDENCIAS_VECINO].map((i) => [i.tipo, i.texto]),
) as Record<TipoIncidencia, string>;

const fechaLarga = new Intl.DateTimeFormat('es-PE', { weekday: 'long', day: 'numeric', month: 'long' });

function desdeIso(fecha: string): Date {
  const [a, m, d] = fecha.slice(0, 10).split('-').map(Number);
  return new Date(a, m - 1, d);
}

/** "Hoy", "Mañana" o "jueves 5 de noviembre". */
export function cuando(fecha: string): string {
  const hoy = new Date();
  const base = new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate());
  const dias = Math.round((desdeIso(fecha).getTime() - base.getTime()) / 86400000);
  if (dias === 0) return 'Hoy';
  if (dias === 1) return 'Mañana';
  if (dias === -1) return 'Ayer';
  const t = fechaLarga.format(desdeIso(fecha));
  return t.charAt(0).toUpperCase() + t.slice(1);
}

export function hora(iso: string): string {
  return new Date(iso).toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' });
}

/** Distancia en metros entre dos puntos (Haversine). */
export function distanciaMetros(a: { latitude: number; longitude: number }, b: { latitude: number; longitude: number }): number {
  const r = 6371000;
  const rad = (g: number) => (g * Math.PI) / 180;
  const dLat = rad(b.latitude - a.latitude);
  const dLng = rad(b.longitude - a.longitude);
  const h = Math.sin(dLat / 2) ** 2 + Math.cos(rad(a.latitude)) * Math.cos(rad(b.latitude)) * Math.sin(dLng / 2) ** 2;
  return 2 * r * Math.asin(Math.sqrt(h));
}

export function textoDistancia(m: number): string {
  return m < 1000 ? `${Math.round(m)} m` : `${(m / 1000).toFixed(1)} km`;
}
