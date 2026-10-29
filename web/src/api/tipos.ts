/** Tipos compartidos con el backend (ver Swagger en /swagger-ui.html). */

export type Rol = 'ADMIN' | 'CONDUCTOR' | 'VECINO';

export interface PersonaResumen {
  id: number;
  nombres: string;
  apellidos: string;
  rol: Rol;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tipoToken: string;
  expiraEnSegundos: number;
  usuario: PersonaResumen;
}

/** Respuesta paginada uniforme: `{ contenido, page, size, totalElements, totalPages }`. */
export interface Pagina<T> {
  contenido: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/** Cuerpo uniforme de error del backend. */
export interface ErrorRespuesta {
  timestamp: string;
  status: number;
  codigo: string;
  mensaje: string;
  detalles: { campo: string; mensaje: string }[];
}

export type Turno = 'MANANA' | 'TARDE';
export type EstadoRuta = 'PROGRAMADA' | 'EN_CURSO' | 'INTERRUMPIDA' | 'COMPLETADA';

export interface RutaResumen {
  id: number;
  fechaProgramada: string;
  turno: Turno;
  estado: EstadoRuta;
  sectorId: number;
  sectorNombre: string;
  cisternaId: number | null;
  cisternaPlaca: string | null;
  conductorId: number | null;
  conductorNombre: string | null;
  viajesPlanificados: number | null;
  totalParadas: number;
  paradasAtendidas: number;
}

export interface Capacidad {
  fecha: string;
  capacidadReferenciaLitros: number;
  cisternasNecesarias: number;
  cisternasDisponibles: number;
  deficit: number;
  hayDeficit: boolean;
  flota: { total: number; disponibles: number; noOperativas: number; autorizacionVencida: number };
}

// ---- Transparencia (RF09): público, solo agregados ----

export type TipoEntrega = 'DOMICILIO' | 'PUNTO_ENTREGA';

export interface ResumenTransparencia {
  desde: string;
  hasta: string;
  sectoresActivos: number;
  sectoresAtendidos: number;
  distritos: number;
  rutas: { total: number; programadas: number; enCurso: number; completadas: number; interrumpidas: number };
  paradas: { total: number; atendidas: number; noAtendidas: number; sinAtender: number; cumplimientoPorcentaje: number | null };
  litrosDistribuidos: number;
  calificacion: { promedio: number | null; total: number };
  incidencias: { reportadas: number; resueltas: number };
  flota: { total: number; disponiblesHoy: number };
}

export interface SectorTransparencia {
  sectorId: number;
  nombre: string;
  distrito: string;
  modalidad: 'PERMANENTE' | 'CONTINGENCIA';
  esZonaAlta: boolean;
  tipoEntrega: TipoEntrega;
  unidadesUso: number;
  frecuenciaDias: number;
  latCentro: number;
  lngCentro: number;
  ultimaAtencion: string | null;
  proximaAtencion: { fecha: string; turno: Turno; estado: EstadoRuta } | null;
  paradasAtendidas: number;
  cumplimientoPorcentaje: number | null;
  calificacionPromedio: number | null;
}

export interface CronogramaItem {
  rutaId: number;
  fecha: string;
  turno: Turno;
  estado: EstadoRuta;
  sectorId: number;
  sectorNombre: string;
  distrito: string;
  tipoEntrega: TipoEntrega;
  cisternaPlaca: string | null;
  totalParadas: number;
  paradasAtendidas: number;
}
