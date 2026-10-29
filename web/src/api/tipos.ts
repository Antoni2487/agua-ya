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

// ---- Rutas (RF01 + RF03) ----

export type TipoParada = 'HOGAR' | 'INSTITUCION' | 'PUNTO_ENTREGA';
export type EstadoParada = 'PENDIENTE' | 'ATENDIDA' | 'NO_ATENDIDA' | 'REASIGNADA';

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

export interface Viaje {
  id: number;
  rutaId: number;
  nroViaje: number;
  estado: 'EN_CARGA' | 'EN_REPARTO' | 'FINALIZADO';
  puntoLlenadoId: number;
  puntoLlenadoNombre: string;
  volumenCargadoLitros: number | null;
  salida: string | null;
  retorno: string | null;
  paradasAtendidas: number;
}

export interface RutaDetalle {
  ruta: RutaResumen;
  puntoLlenadoId: number | null;
  puntoLlenadoNombre: string | null;
  viajes: Viaje[];
  paradas: Parada[];
}

export interface SugerenciasAsignacion {
  rutaId: number;
  litrosPorVisita: number;
  maxViajesDiarios: number;
  cisternas: { id: number; placa: string; capacidadLitros: number; viajesNecesarios: number; excedeMaximoDiario: boolean }[];
  conductores: { id: number; nombres: string; apellidos: string; turno: Turno; mismoTurno: boolean; distanciaKm: number | null }[];
}

export interface Sector {
  id: number;
  nombre: string;
  distrito: string;
  modalidad: 'PERMANENTE' | 'CONTINGENCIA';
  esZonaAlta: boolean;
  unidadesUso: number;
  dotacionLitros: number;
  frecuenciaDias: number;
  maxViajesDiarios: number;
  latCentro: number;
  lngCentro: number;
  tipoEntrega: TipoEntrega;
  activo: boolean;
}

export interface PuntoLlenado {
  id: number;
  nombre: string;
  tipo: 'PLANTA' | 'SURTIDOR' | 'POZO';
  direccion: string | null;
  latitud: number;
  longitud: number;
  activo: boolean;
}

// ---- Tiempo real (RF04, RF06) ----

export interface Posicion {
  rutaId: number;
  latitud: number;
  longitud: number;
  registradoEn: string;
}

export interface Recorrido {
  rutaId: number;
  posiciones: Posicion[];
  intentosFallidos: {
    paradaId: number;
    ordenParada: number;
    latitud: number | null;
    longitud: number | null;
    distanciaMetros: number | null;
    registradoEn: string;
  }[];
}

export type TipoEventoRuta =
  | 'RUTA_INICIADA' | 'PARADA_ATENDIDA' | 'PARADA_NO_ATENDIDA' | 'VIAJE_INICIADO' | 'VIAJE_FINALIZADO'
  | 'RUTA_COMPLETADA' | 'RUTA_INTERRUMPIDA' | 'PARADAS_REASIGNADAS';

export interface EventoRuta {
  tipo: TipoEventoRuta;
  rutaId: number;
  paradaId: number | null;
  viajeId: number | null;
  registradoEn: string;
}

export type TipoIncidencia =
  | 'AVERIA' | 'ACCESO_BLOQUEADO' | 'AGUA_AGOTADA' | 'NO_LLEGO' | 'LLEGO_TARDE' | 'YA_PASO' | 'VOLUMEN_INSUFICIENTE' | 'OTRO';

export interface Incidencia {
  id: number;
  rutaId: number;
  sectorNombre: string;
  estadoRuta: EstadoRuta;
  paradaId: number | null;
  ordenParada: number | null;
  tipo: TipoIncidencia;
  descripcion: string | null;
  estado: 'ABIERTA' | 'REASIGNADA' | 'CERRADA';
  rutaDestinoId: number | null;
  reportadoPorId: number;
  reportadoPorNombre: string;
  reportadoPorRol: Rol;
  creadoEn: string;
  resueltaPorNombre: string | null;
  resueltoEn: string | null;
}

// ---- Reasignación (RF07), capacidad (RF02) y calificaciones (RF08) ----

export interface SugerenciasReasignacion {
  incidenciaId: number;
  rutaOrigenId: number;
  paradasPendientes: number;
  rutas: {
    rutaId: number;
    fecha: string;
    turno: Turno;
    estado: EstadoRuta;
    cisternaPlaca: string | null;
    capacidadLitros: number | null;
    conductorNombre: string | null;
    distanciaKm: number | null;
    paradasPendientes: number;
  }[];
}

export interface Reasignacion {
  incidencia: Incidencia;
  rutaOrigenId: number;
  rutaDestinoId: number;
  paradasReasignadas: number;
  paradasYaEnDestino: number;
}

export interface SectorCapacidad {
  sectorId: number;
  nombre: string;
  distrito: string;
  esZonaAlta: boolean;
  unidadesUso: number;
  dotacionLitros: number;
  frecuenciaDias: number;
  litrosPorVisita: number;
  viajesNecesarios: number;
  maxViajesDiarios: number;
  cisternasNecesarias: number;
  deficit: number;
}

export interface CapacidadDetalle extends Capacidad {
  sectores: SectorCapacidad[];
}

export interface Calificacion {
  id: number;
  paradaId: number;
  rutaId: number;
  fecha: string;
  turno: Turno;
  sectorId: number;
  sectorNombre: string;
  conductorNombre: string | null;
  cisternaPlaca: string | null;
  vecinoNombre: string;
  estrellas: number;
  comentario: string | null;
  creadoEn: string;
}

export interface ResumenCalificaciones {
  total: number;
  promedio: number | null;
  distribucion: Record<string, number>;
  conductores: { conductorId: number; nombre: string; promedio: number; total: number }[];
}
