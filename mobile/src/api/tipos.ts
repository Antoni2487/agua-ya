// Tipos de la API de AguaYa que usa la app (los mismos records del backend).

export type Rol = 'ADMIN' | 'CONDUCTOR' | 'VECINO';
export type Turno = 'MANANA' | 'TARDE';
export type EstadoRuta = 'PROGRAMADA' | 'EN_CURSO' | 'INTERRUMPIDA' | 'COMPLETADA';
export type EstadoParada = 'PENDIENTE' | 'ATENDIDA' | 'NO_ATENDIDA' | 'REASIGNADA';
export type TipoParada = 'HOGAR' | 'INSTITUCION' | 'PUNTO_ENTREGA';
export type EstadoViaje = 'EN_CARGA' | 'EN_REPARTO' | 'FINALIZADO';

export interface ErrorRespuesta {
  timestamp: string;
  status: number;
  codigo: string;
  mensaje: string;
  detalles: { campo: string; mensaje: string }[];
}

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

export interface SolicitarCodigoResponse {
  mensaje: string;
  telefonoEnmascarado?: string;
  expiraEn: string;
  /** Solo en modo demo (el backend expone el código); en producción llega por SMS. */
  codigo?: string | null;
}

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

export interface Viaje {
  id: number;
  rutaId: number;
  nroViaje: number;
  estado: EstadoViaje;
  puntoLlenadoId: number;
  puntoLlenadoNombre: string;
  volumenCargadoLitros: number | null;
  salida: string | null;
  retorno: string | null;
  paradasAtendidas: number;
}

export interface Parada {
  id: number;
  orden: number;
  tipo: TipoParada;
  prioridad: 'CRITICA' | 'NORMAL';
  estado: EstadoParada;
  nombre: string;
  referencia: string | null;
  latitud: number;
  longitud: number;
  viajeId: number | null;
}

export interface RutaDetalle {
  ruta: RutaResumen;
  puntoLlenadoId: number;
  puntoLlenadoNombre: string;
  viajes: Viaje[];
  paradas: Parada[];
}

export interface MiParada {
  paradaId: number;
  tipo: TipoParada;
  nombre: string;
  referencia: string | null;
  orden: number;
  estado: EstadoParada;
  pendientesAntes: number;
  miCalificacion: number | null;
}

export interface Atencion {
  rutaId: number;
  fecha: string;
  turno: Turno;
  estado: EstadoRuta;
  totalParadas: number;
  paradasAtendidas: number;
  miParada?: MiParada;
}

export interface AtencionVecino {
  proxima: Atencion | null;
  ultima: Atencion | null;
}

export interface VecinoPerfil {
  id: number;
  dni: string;
  nombres: string;
  apellidos: string;
  telefono: string;
  direccionReferencia: string | null;
  latitud: number;
  longitud: number;
  sector: { id: number; nombre: string; distrito: string; tipoEntrega: 'DOMICILIO' | 'PUNTO_ENTREGA' } | null;
}

export interface Posicion {
  rutaId: number;
  latitud: number;
  longitud: number;
  registradoEn: string;
}

export type TipoIncidencia =
  | 'AVERIA' | 'ACCESO_BLOQUEADO' | 'AGUA_AGOTADA'
  | 'NO_LLEGO' | 'LLEGO_TARDE' | 'YA_PASO' | 'VOLUMEN_INSUFICIENTE'
  | 'OTRO';

export interface Incidencia {
  id: number;
  rutaId: number;
  sectorNombre: string;
  tipo: TipoIncidencia;
  descripcion: string | null;
  estado: 'ABIERTA' | 'REASIGNADA' | 'CERRADA';
  creadoEn: string;
}

export interface Pagina<T> {
  contenido: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
