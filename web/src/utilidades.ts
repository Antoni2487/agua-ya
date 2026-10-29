import type { EstadoRuta, Turno } from './api/tipos';

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
