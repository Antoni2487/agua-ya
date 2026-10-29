import type { Cisterna, Conductor, Institucion, PuntoEntrega, PuntoLlenado, Sector } from '../../../api/tipos';
import { fechaCorta, numero, TURNOS, volumen } from '../../../utilidades';
import type { Campo, Catalogo } from './modelo';

export const MODALIDADES = { PERMANENTE: 'Permanente', CONTINGENCIA: 'Contingencia' };
export const TIPOS_ENTREGA = { DOMICILIO: 'Casa por casa', PUNTO_ENTREGA: 'Punto de entrega' };
export const TIPOS_INSTITUCION = { SALUD: 'Salud', BOMBEROS: 'Bomberos', EDUCACION: 'Educación', OTRO: 'Otro' };
export const TIPOS_PUNTO_LLENADO = { PLANTA: 'Planta', SURTIDOR: 'Surtidor', POZO: 'Pozo' };
export const ESTADOS_VEHICULO = { OPERATIVA: 'Operativa', MANTENIMIENTO: 'Mantenimiento', AVERIADA: 'Averiada' };
export const DISPONIBILIDAD = { DISPONIBLE: 'Disponible', EN_RUTA: 'En ruta', INACTIVO: 'Inactivo' };

const activo: Campo = { nombre: 'activo', etiqueta: 'Activo', tipo: 'casilla', solo: 'editar' };
const latitud = (nombre = 'latitud'): Campo => ({
  nombre, etiqueta: 'Latitud', tipo: 'coordenada', atributos: { min: -90, max: 90, step: 'any' },
});
const longitud = (nombre = 'longitud'): Campo => ({
  nombre, etiqueta: 'Longitud', tipo: 'coordenada', atributos: { min: -180, max: 180, step: 'any' },
});
const estado = (item: { activo: boolean }) =>
  item.activo ? <span className="etiqueta etiqueta-activo">Activo</span> : <span className="etiqueta etiqueta-inactivo">Inactivo</span>;
const desactivar = { etiqueta: 'Desactivar', disponible: (item: { activo: boolean }) => item.activo };

const sectores: Catalogo<Sector> = {
  ruta: 'sectores',
  titulo: 'Sectores',
  singular: { nombre: 'sector', articulo: 'el' },
  endpoint: '/api/admin/sectores',
  orden: 'nombre',
  descripcion:
    'Zonas que atiende EPSEL con cisterna. Las unidades de uso, la dotación y la frecuencia definen la capacidad necesaria (RF02); el centro asigna a cada vecino nuevo su sector más cercano.',
  filtros: [{ nombre: 'activo', etiqueta: 'Estado', opciones: 'activo' }],
  columnas: [
    { titulo: 'Nombre', valor: (s) => <>{s.nombre}{s.esZonaAlta && <span className="etiqueta etiqueta-zona-alta">Zona alta</span>}</> },
    { titulo: 'Distrito', valor: (s) => s.distrito },
    { titulo: 'Modalidad', valor: (s) => MODALIDADES[s.modalidad] },
    { titulo: 'Entrega', valor: (s) => TIPOS_ENTREGA[s.tipoEntrega] },
    { titulo: 'Unidades de uso', valor: (s) => numero(s.unidadesUso) },
    { titulo: 'Frecuencia', valor: (s) => (s.frecuenciaDias === 1 ? 'Diaria' : `Cada ${s.frecuenciaDias} días`) },
    { titulo: 'Viajes/día', valor: (s) => s.maxViajesDiarios },
    { titulo: 'Estado', valor: estado },
  ],
  campos: [
    { nombre: 'nombre', etiqueta: 'Nombre', atributos: { maxLength: 100 } },
    { nombre: 'distrito', etiqueta: 'Distrito', atributos: { maxLength: 60 } },
    { nombre: 'modalidad', etiqueta: 'Modalidad', tipo: 'select', opciones: MODALIDADES },
    { nombre: 'tipoEntrega', etiqueta: 'Tipo de entrega', tipo: 'select', opciones: TIPOS_ENTREGA,
      ayuda: 'Casa por casa genera una parada por vecino; punto de entrega, una por punto fijo.' },
    { nombre: 'unidadesUso', etiqueta: 'Unidades de uso', tipo: 'numero', atributos: { min: 0 } },
    { nombre: 'dotacionLitros', etiqueta: 'Dotación (L por unidad y día)', tipo: 'numero', opcional: true,
      atributos: { min: 1, placeholder: '80' }, ayuda: 'Mínimo SUNASS: 80 L. Vacío usa 80.' },
    { nombre: 'frecuenciaDias', etiqueta: 'Frecuencia (días entre visitas)', tipo: 'numero', atributos: { min: 1 } },
    { nombre: 'maxViajesDiarios', etiqueta: 'Máx. viajes por cisterna al día', tipo: 'numero', atributos: { min: 1 } },
    latitud('latCentro'),
    longitud('lngCentro'),
    { nombre: 'esZonaAlta', etiqueta: 'Zona alta (difícil acceso)', tipo: 'casilla' },
    activo,
  ],
  ubicacion: ['latCentro', 'lngCentro'],
  baja: { ...desactivar, confirmacion: (s) => `¿Desactivar ${s.nombre}? No se le asignarán vecinos nuevos ni se podrán programar rutas.` },
  activo: (s) => s.activo,
  nombreDe: (s) => s.nombre,
};

const instituciones: Catalogo<Institucion> = {
  ruta: 'instituciones',
  titulo: 'Instituciones críticas',
  singular: { nombre: 'institución', articulo: 'la' },
  endpoint: '/api/admin/instituciones',
  orden: 'nombre',
  descripcion: 'Postas, bomberos y colegios del sector: entran como paradas CRÍTICAS al inicio de cada ruta.',
  filtros: [
    { nombre: 'sectorId', etiqueta: 'Sector', opciones: 'sector' },
    { nombre: 'tipo', etiqueta: 'Tipo', opciones: TIPOS_INSTITUCION },
    { nombre: 'activo', etiqueta: 'Estado', opciones: 'activo' },
  ],
  columnas: [
    { titulo: 'Nombre', valor: (i) => i.nombre },
    { titulo: 'Tipo', valor: (i) => TIPOS_INSTITUCION[i.tipo] },
    { titulo: 'Sector', valor: (i) => i.sectorNombre },
    { titulo: 'Estado', valor: estado },
  ],
  campos: [
    { nombre: 'nombre', etiqueta: 'Nombre', atributos: { maxLength: 150 } },
    { nombre: 'tipo', etiqueta: 'Tipo', tipo: 'select', opciones: TIPOS_INSTITUCION },
    { nombre: 'sectorId', etiqueta: 'Sector', tipo: 'sector' },
    latitud(),
    longitud(),
    activo,
  ],
  ubicacion: ['latitud', 'longitud'],
  baja: { ...desactivar, confirmacion: (i) => `¿Desactivar ${i.nombre}? Dejará de incluirse en las rutas nuevas.` },
  activo: (i) => i.activo,
  nombreDe: (i) => i.nombre,
};

const puntosEntrega: Catalogo<PuntoEntrega> = {
  ruta: 'puntos-entrega',
  titulo: 'Puntos de entrega',
  singular: { nombre: 'punto de entrega', articulo: 'el' },
  endpoint: '/api/admin/puntos-entrega',
  orden: 'nombre',
  descripcion: 'Lugares fijos donde se detiene la cisterna en los sectores con entrega por punto (no casa por casa).',
  filtros: [
    { nombre: 'sectorId', etiqueta: 'Sector', opciones: 'sector' },
    { nombre: 'activo', etiqueta: 'Estado', opciones: 'activo' },
  ],
  columnas: [
    { titulo: 'Nombre', valor: (p) => p.nombre },
    { titulo: 'Sector', valor: (p) => p.sectorNombre },
    { titulo: 'Referencia', valor: (p) => p.referencia ?? '—' },
    { titulo: 'Estado', valor: estado },
  ],
  campos: [
    { nombre: 'nombre', etiqueta: 'Nombre', atributos: { maxLength: 150 } },
    { nombre: 'sectorId', etiqueta: 'Sector', tipo: 'sector' },
    { nombre: 'referencia', etiqueta: 'Referencia', opcional: true, atributos: { maxLength: 255, placeholder: 'Frente a la losa deportiva' } },
    latitud(),
    longitud(),
    activo,
  ],
  ubicacion: ['latitud', 'longitud'],
  baja: { ...desactivar, confirmacion: (p) => `¿Desactivar ${p.nombre}? Dejará de incluirse en las rutas nuevas.` },
  activo: (p) => p.activo,
  nombreDe: (p) => p.nombre,
};

const puntosLlenado: Catalogo<PuntoLlenado> = {
  ruta: 'puntos-llenado',
  titulo: 'Puntos de llenado',
  singular: { nombre: 'punto de llenado', articulo: 'el' },
  endpoint: '/api/admin/puntos-llenado',
  orden: 'nombre',
  descripcion: 'Plantas, surtidores y pozos donde cargan las cisternas; las paradas se ordenan desde el punto de llenado de la ruta.',
  filtros: [
    { nombre: 'tipo', etiqueta: 'Tipo', opciones: TIPOS_PUNTO_LLENADO },
    { nombre: 'activo', etiqueta: 'Estado', opciones: 'activo' },
  ],
  columnas: [
    { titulo: 'Nombre', valor: (p) => p.nombre },
    { titulo: 'Tipo', valor: (p) => TIPOS_PUNTO_LLENADO[p.tipo] },
    { titulo: 'Dirección', valor: (p) => p.direccion ?? '—' },
    { titulo: 'Estado', valor: estado },
  ],
  campos: [
    { nombre: 'nombre', etiqueta: 'Nombre', atributos: { maxLength: 150 } },
    { nombre: 'tipo', etiqueta: 'Tipo', tipo: 'select', opciones: TIPOS_PUNTO_LLENADO },
    { nombre: 'direccion', etiqueta: 'Dirección', opcional: true, atributos: { maxLength: 255 } },
    latitud(),
    longitud(),
    activo,
  ],
  ubicacion: ['latitud', 'longitud'],
  baja: { ...desactivar, confirmacion: (p) => `¿Desactivar ${p.nombre}? No se podrá elegir al programar rutas.` },
  activo: (p) => p.activo,
  nombreDe: (p) => p.nombre,
};

const cisternas: Catalogo<Cisterna> = {
  ruta: 'cisternas',
  titulo: 'Cisternas',
  singular: { nombre: 'cisterna', articulo: 'la' },
  endpoint: '/api/admin/cisternas',
  orden: 'placa',
  descripcion:
    'Solo se asignan cisternas OPERATIVAS con autorización sanitaria vigente (DIRESA/GERESA) en la fecha de la ruta. Para retirarla de servicio cambia su estado; solo se elimina si nunca tuvo rutas.',
  filtros: [{ nombre: 'estado', etiqueta: 'Estado', opciones: ESTADOS_VEHICULO }],
  columnas: [
    { titulo: 'Placa', valor: (c) => <span className="placa">{c.placa}</span> },
    { titulo: 'Capacidad', valor: (c) => volumen(c.capacidadLitros) },
    { titulo: 'Estado', valor: (c) => <span className={`etiqueta etiqueta-vehiculo-${c.estadoVehiculo.toLowerCase()}`}>{ESTADOS_VEHICULO[c.estadoVehiculo]}</span> },
    {
      titulo: 'Autorización sanitaria',
      valor: (c) => (
        <>
          {c.nroAutorizacionSanitaria ?? 'Sin número'}
          {c.venceAutorizacion && <span className="texto-suave"> · vence {fechaCorta(c.venceAutorizacion)}</span>}
          {!c.autorizacionVigente && <span className="etiqueta etiqueta-inactivo etiqueta-vencida">Vencida</span>}
        </>
      ),
    },
  ],
  campos: [
    { nombre: 'placa', etiqueta: 'Placa', atributos: { pattern: '[A-Za-z0-9]{3}-\\d{3}', placeholder: 'ABC-123', title: 'Formato ABC-123' } },
    { nombre: 'capacidadLitros', etiqueta: 'Capacidad (L)', tipo: 'numero', atributos: { min: 1, max: 50_000 } },
    { nombre: 'estadoVehiculo', etiqueta: 'Estado', tipo: 'select', opciones: ESTADOS_VEHICULO },
    { nombre: 'nroAutorizacionSanitaria', etiqueta: 'N.º autorización sanitaria', opcional: true, atributos: { maxLength: 50 } },
    { nombre: 'venceAutorizacion', etiqueta: 'Vence la autorización', tipo: 'fecha', opcional: true },
  ],
  baja: {
    etiqueta: 'Eliminar',
    disponible: () => true,
    confirmacion: (c) => `¿Eliminar la cisterna ${c.placa}? Solo es posible si nunca tuvo rutas.`,
  },
  nombreDe: (c) => c.placa,
};

const conductores: Catalogo<Conductor> = {
  ruta: 'conductores',
  titulo: 'Conductores',
  singular: { nombre: 'conductor', articulo: 'el' },
  endpoint: '/api/admin/conductores',
  orden: 'apellidos',
  descripcion:
    'Cuentas de los conductores: ingresan a la app con su DNI y la contraseña que les des. Desactivar cierra sus sesiones; "En ruta" lo asigna el sistema al iniciar una ruta.',
  filtros: [
    { nombre: 'estado', etiqueta: 'Disponibilidad', opciones: DISPONIBILIDAD },
    { nombre: 'turno', etiqueta: 'Turno', opciones: TURNOS },
    { nombre: 'activo', etiqueta: 'Estado', opciones: 'activo' },
  ],
  columnas: [
    { titulo: 'Nombre', valor: (c) => `${c.apellidos}, ${c.nombres}` },
    { titulo: 'DNI', valor: (c) => c.dni },
    { titulo: 'Teléfono', valor: (c) => c.telefono },
    { titulo: 'Licencia', valor: (c) => c.licencia },
    { titulo: 'Turno', valor: (c) => (c.turno ? TURNOS[c.turno] : '—') },
    { titulo: 'Disponibilidad', valor: (c) => <span className={`etiqueta etiqueta-disponibilidad-${c.estadoDisponibilidad.toLowerCase()}`}>{DISPONIBILIDAD[c.estadoDisponibilidad]}</span> },
    { titulo: 'Estado', valor: estado },
  ],
  campos: [
    { nombre: 'dni', etiqueta: 'DNI', solo: 'crear', atributos: { pattern: '\\d{8}', inputMode: 'numeric', maxLength: 8, title: '8 dígitos' } },
    { nombre: 'nombres', etiqueta: 'Nombres', atributos: { maxLength: 100 } },
    { nombre: 'apellidos', etiqueta: 'Apellidos', atributos: { maxLength: 100 } },
    { nombre: 'telefono', etiqueta: 'Celular', atributos: { pattern: '(\\+51)?9\\d{8}', inputMode: 'tel', placeholder: '987654321', title: 'Celular peruano de 9 dígitos' } },
    { nombre: 'email', etiqueta: 'Correo', opcional: true, atributos: { type: 'email', maxLength: 150 } },
    { nombre: 'licencia', etiqueta: 'Licencia', atributos: { maxLength: 20 } },
    { nombre: 'turno', etiqueta: 'Turno', tipo: 'select', opciones: TURNOS, sinValor: 'Sin turno fijo' },
    {
      nombre: 'estadoDisponibilidad', etiqueta: 'Disponibilidad', tipo: 'select', solo: 'editar',
      opciones: { DISPONIBLE: DISPONIBILIDAD.DISPONIBLE, INACTIVO: DISPONIBILIDAD.INACTIVO }, sinValor: 'Sin cambio',
      // EN_RUTA lo asigna el sistema: se envía null para no tocarlo.
      inicial: (c: Conductor) => (c.estadoDisponibilidad === 'EN_RUTA' ? '' : c.estadoDisponibilidad),
    },
    {
      nombre: 'password', etiqueta: 'Contraseña', tipo: 'password', opcional: 'editar',
      atributos: { minLength: 8, maxLength: 72, autoComplete: 'new-password' },
      ayuda: 'Entre 8 y 72 caracteres. Al editar, déjala vacía para no cambiarla (cambiarla cierra sus sesiones).',
    },
    activo,
  ],
  baja: { ...desactivar, confirmacion: (c) => `¿Desactivar a ${c.nombres} ${c.apellidos}? No podrá ingresar y se cerrarán sus sesiones.` },
  activo: (c) => c.activo,
  nombreDe: (c) => `${c.nombres} ${c.apellidos}`,
};

export const CATALOGOS = [sectores, instituciones, puntosEntrega, puntosLlenado, cisternas, conductores] as unknown as Catalogo<never>[];
