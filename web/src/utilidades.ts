import type {
  EstadoParada, EstadoRuta, Incidencia, Rol, TipoEventoRuta, TipoIncidencia, TipoParada, Turno,
} from './api/tipos';

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

export const TIPOS_INCIDENCIA: Record<TipoIncidencia, string> = {
  AVERIA: 'Avería',
  ACCESO_BLOQUEADO: 'Acceso bloqueado',
  AGUA_AGOTADA: 'Agua agotada',
  NO_LLEGO: 'No llegó',
  LLEGO_TARDE: 'Llegó tarde',
  YA_PASO: 'Ya pasó',
  VOLUMEN_INSUFICIENTE: 'Volumen insuficiente',
  OTRO: 'Otro',
};

export const EVENTOS_RUTA: Record<TipoEventoRuta, string> = {
  RUTA_INICIADA: 'inició su recorrido',
  PARADA_ATENDIDA: 'atendió una parada',
  PARADA_NO_ATENDIDA: 'marcó una parada como no atendida',
  VIAJE_INICIADO: 'salió cargada en un nuevo viaje',
  VIAJE_FINALIZADO: 'terminó un viaje',
  RUTA_COMPLETADA: 'completó la ruta',
  RUTA_INTERRUMPIDA: 'se interrumpió',
  PARADAS_REASIGNADAS: 'recibió paradas reasignadas',
};

/** "hace 40 s", "hace 3 min", "hace 2 h" respecto de ahora. */
export function hace(iso: string, ahora: number = Date.now()): string {
  const segundos = Math.max(0, Math.round((ahora - new Date(iso).getTime()) / 1000));
  if (segundos < 60) return `hace ${segundos} s`;
  if (segundos < 3600) return `hace ${Math.floor(segundos / 60)} min`;
  return `hace ${Math.floor(segundos / 3600)} h`;
}

export const ESTADOS_INCIDENCIA: Record<Incidencia['estado'], string> = {
  ABIERTA: 'Abierta',
  REASIGNADA: 'Reasignada',
  CERRADA: 'Cerrada',
};

export const ROLES: Record<Rol, string> = { ADMIN: 'Administrador', CONDUCTOR: 'Conductor', VECINO: 'Vecino' };

/** "29/10/2026 08:35" en hora de Lima. */
export function fechaHora(iso: string): string {
  const partes = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'America/Lima', year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false,
  }).formatToParts(new Date(iso));
  const p = (tipo: string) => partes.find((x) => x.type === tipo)?.value ?? '';
  return `${p('day')}/${p('month')}/${p('year')} ${p('hour')}:${p('minute')}`;
}
