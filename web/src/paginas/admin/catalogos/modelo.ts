import type { InputHTMLAttributes, ReactNode } from 'react';

import type { Sector } from '../../../api/tipos';

/** Valores del formulario tal como los escriben los inputs; se convierten al cuerpo del backend al guardar. */
export type Valores = Record<string, string | boolean>;

export interface Campo {
  nombre: string;
  etiqueta: string;
  /** `sector` es un desplegable con todos los sectores; `coordenada` va además al selector del mapa. */
  tipo?: 'texto' | 'numero' | 'coordenada' | 'fecha' | 'password' | 'select' | 'sector' | 'casilla';
  opciones?: Record<string, string>;
  /** Texto de la opción vacía de un select opcional (se envía null). */
  sinValor?: string;
  /** `true`: nunca obligatorio; `editar`: obligatorio solo al crear (p. ej. la contraseña). */
  opcional?: true | 'editar';
  /** Campo que solo existe al crear (DNI) o al editar (activo). */
  solo?: 'crear' | 'editar';
  ayuda?: string;
  atributos?: InputHTMLAttributes<HTMLInputElement>;
  /** Valor inicial al editar, si no es el mismo campo de la respuesta. */
  inicial?: (item: never) => string | boolean;
}

export interface Filtro {
  nombre: string;
  etiqueta: string;
  /** `sector` usa la lista de sectores; `activo` ofrece activos/inactivos. */
  opciones: Record<string, string> | 'sector' | 'activo';
}

export interface Catalogo<T extends { id: number }> {
  ruta: string;
  titulo: string;
  /** Con artículo: "el sector", "la cisterna"; se usa en botones y mensajes. */
  singular: { nombre: string; articulo: 'el' | 'la' };
  endpoint: string;
  orden: string;
  descripcion: string;
  filtros: Filtro[];
  columnas: { titulo: string; valor: (item: T) => ReactNode }[];
  campos: Campo[];
  /** Campos de latitud y longitud que se eligen también haciendo clic en el mapa. */
  ubicacion?: [string, string];
  /** Baja lógica (DELETE desactiva) o eliminación real (cisternas sin rutas). */
  baja: { etiqueta: string; confirmacion: (item: T) => string; disponible: (item: T) => boolean };
  activo?: (item: T) => boolean;
  /** Nombre para mensajes ("Sector P.J. Atusparias guardado"). */
  nombreDe: (item: T) => string;
}

/** Campos visibles según se cree o edite. */
export function camposDe(catalogo: Catalogo<never>, editando: boolean): Campo[] {
  return catalogo.campos.filter((c) => !c.solo || c.solo === (editando ? 'editar' : 'crear'));
}

export function esObligatorio(campo: Campo, editando: boolean): boolean {
  if (campo.tipo === 'casilla' || campo.opcional === true) return false;
  return !(campo.opcional === 'editar' && editando);
}

/** Valores iniciales: vacíos al crear (o el primer valor de un select obligatorio), los del registro al editar. */
export function valoresIniciales(catalogo: Catalogo<never>, item: Record<string, unknown> | null): Valores {
  const valores: Valores = {};
  for (const campo of camposDe(catalogo, item !== null)) {
    if (item) {
      const v = campo.inicial ? campo.inicial(item as never) : item[campo.nombre];
      valores[campo.nombre] = typeof v === 'boolean' ? v : v === null || v === undefined ? '' : String(v);
    } else if (campo.tipo === 'casilla') {
      valores[campo.nombre] = false;
    } else if (campo.tipo === 'select' && !campo.sinValor && campo.opciones) {
      valores[campo.nombre] = Object.keys(campo.opciones)[0]!;
    } else {
      valores[campo.nombre] = '';
    }
  }
  return valores;
}

/** Cuerpo del POST/PUT: números como números, vacíos como null (el backend aplica su valor por defecto). */
export function aCuerpo(catalogo: Catalogo<never>, valores: Valores, editando: boolean): Record<string, unknown> {
  const cuerpo: Record<string, unknown> = {};
  for (const campo of camposDe(catalogo, editando)) {
    const v = valores[campo.nombre];
    if (typeof v === 'boolean') cuerpo[campo.nombre] = v;
    else if (v === undefined || v.trim() === '') cuerpo[campo.nombre] = null;
    else if (campo.tipo === 'numero' || campo.tipo === 'coordenada' || campo.tipo === 'sector') cuerpo[campo.nombre] = Number(v);
    else cuerpo[campo.nombre] = campo.tipo === 'password' ? v : v.trim();
  }
  return cuerpo;
}

/** Centro del sector elegido en el formulario, para encuadrar el mapa antes de marcar la ubicación. */
export function centroDelSector(catalogo: Catalogo<never>, valores: Valores, sectores: Sector[] | undefined): [number, number] | null {
  const campo = catalogo.campos.find((c) => c.tipo === 'sector');
  const sector = campo && sectores?.find((s) => String(s.id) === valores[campo.nombre]);
  return sector ? [Number(sector.latCentro), Number(sector.lngCentro)] : null;
}
