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
