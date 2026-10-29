import type { EstadoParada, EstadoRuta, TipoParada, Turno } from './api/tipos';

/** Fecha de hoy en Lima (zona del negocio), como yyyy-MM-dd. */
export function hoyEnLima(ahora: Date = new Date()): string {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Lima' }).format(ahora);
}

export const TURNOS: Record<Turno, string> = { MANANA: 'Mañana', TARDE: 'Tarde' };

export const ESTADOS_RUTA: Record<EstadoRuta, string> = {
  PROGRAMADA: 'Programada',
  EN_CURSO: 'En curso',
  INTERRUMPIDA: 'Interrumpida',
  COMPLETADA: 'Completada',
};

/** 2026-10-29 → 29/10/2026 sin conversiones de zona horaria. */
export function fechaCorta(iso: string): string {
  const [anio, mes, dia] = iso.slice(0, 10).split('-');
  return `${dia}/${mes}/${anio}`;
}

/** Suma (o resta) días a una fecha yyyy-MM-dd sin depender de la zona horaria del navegador. */
export function sumarDias(iso: string, dias: number): string {
  const fecha = new Date(`${iso.slice(0, 10)}T00:00:00Z`);
  fecha.setUTCDate(fecha.getUTCDate() + dias);
  return fecha.toISOString().slice(0, 10);
}

/** "jueves 30/10" para encabezados de cronograma. */
export function diaSemana(iso: string): string {
  const fecha = new Date(`${iso.slice(0, 10)}T12:00:00Z`);
  const dia = new Intl.DateTimeFormat('es-PE', { weekday: 'long', timeZone: 'UTC' }).format(fecha);
  return `${dia} ${fechaCorta(iso).slice(0, 5)}`;
}

const formatoNumero = new Intl.NumberFormat('es-PE');

export function numero(valor: number): string {
  return formatoNumero.format(valor);
}

/** Litros en m³ cuando son muchos: 1 250 000 L → "1 250 m³". */
export function volumen(litros: number): string {
  return litros >= 100_000 ? `${numero(Math.round(litros / 1000))} m³` : `${numero(litros)} L`;
}

/** "1 ruta completada" / "3 rutas completadas". */
export function contar(cantidad: number, singular: string, plural: string): string {
  return `${numero(cantidad)} ${cantidad === 1 ? singular : plural}`;
}

export function porcentaje(valor: number | null): string {
  return valor === null ? '—' : `${valor.toLocaleString('es-PE', { maximumFractionDigits: 1 })} %`;
}


export const TIPOS_PARADA: Record<TipoParada, string> = {
  HOGAR: 'Hogar',
  INSTITUCION: 'Institución',
  PUNTO_ENTREGA: 'Punto de entrega',
};

export const ESTADOS_PARADA: Record<EstadoParada, string> = {
  PENDIENTE: 'Pendiente',
  ATENDIDA: 'Atendida',
  NO_ATENDIDA: 'No atendida',
  REASIGNADA: 'Reasignada',
};

/** Hora local de Lima de un instante ISO: "08:35". */
export function hora(iso: string | null): string {
  if (!iso) return '—';
  return new Intl.DateTimeFormat('es-PE', { hour: '2-digit', minute: '2-digit', timeZone: 'America/Lima', hour12: false })
    .format(new Date(iso));
}
