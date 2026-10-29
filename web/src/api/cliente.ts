import type { AuthResponse, ErrorRespuesta } from './tipos';

/** URL del backend sin "/" final (VITE_API_URL). */
export const API_URL = (import.meta.env.VITE_API_URL ?? 'http://localhost:8080').replace(/\/$/, '');

const CLAVE_REFRESH = 'aguaya.refreshToken';

/** Error de negocio o validación del backend, con su código (p. ej. RUTA_DUPLICADA). */
export class ErrorApi extends Error {
  readonly status: number;
  readonly codigo: string;
  readonly detalles: ErrorRespuesta['detalles'];

  constructor(status: number, codigo: string, mensaje: string, detalles: ErrorRespuesta['detalles'] = []) {
    super(mensaje);
    this.name = 'ErrorApi';
    this.status = status;
    this.codigo = codigo;
    this.detalles = detalles;
  }

  /** Mensaje para mostrar: el del backend más los errores por campo, si los hay. */
  mensaje(): string {
    if (this.detalles.length === 0) return this.message;
    return `${this.message}: ${this.detalles.map((d) => `${d.campo} ${d.mensaje}`).join('; ')}`;
  }
}

// El access token (15 min) vive solo en memoria; el refresh token (30 días) en localStorage
// para sobrevivir a recargar la página. Se rota en cada uso.
let accessToken: string | null = null;
let refrescando: Promise<AuthResponse | null> | null = null;
const oyentes = new Set<(sesion: AuthResponse | null) => void>();

function leerRefresh(): string | null {
  try {
    return localStorage.getItem(CLAVE_REFRESH);
  } catch {
    return null;
  }
}

function guardarRefresh(token: string | null) {
  try {
    if (token) localStorage.setItem(CLAVE_REFRESH, token);
    else localStorage.removeItem(CLAVE_REFRESH);
  } catch {
    // Sin almacenamiento (modo privado): la sesión dura lo que la pestaña.
  }
}

export function establecerSesion(sesion: AuthResponse | null) {
  accessToken = sesion?.accessToken ?? null;
  guardarRefresh(sesion?.refreshToken ?? null);
  oyentes.forEach((oyente) => oyente(sesion));
}

/** Avisa cuando la sesión cambia (login, refresh, logout o refresh vencido). */
export function alCambiarSesion(oyente: (sesion: AuthResponse | null) => void): () => void {
  oyentes.add(oyente);
  return () => oyentes.delete(oyente);
}

export function hayRefreshGuardado(): boolean {
  return leerRefresh() !== null;
}

export function refreshGuardado(): string | null {
  return leerRefresh();
}

/** El token actual, para el CONNECT del WebSocket. */
export function tokenActual(): string | null {
  return accessToken;
}

/** Pide un access token nuevo con el refresh token. Una sola petición aunque fallen varias a la vez. */
export function refrescarSesion(): Promise<AuthResponse | null> {
  const refreshToken = leerRefresh();
  if (!refreshToken) return Promise.resolve(null);
  refrescando ??= fetch(`${API_URL}/api/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  })
    .then(async (r) => {
      const sesion = r.ok ? ((await r.json()) as AuthResponse) : null;
      establecerSesion(sesion);
      return sesion;
    })
    .catch(() => null)
    .finally(() => {
      refrescando = null;
    });
  return refrescando;
}

export interface Opciones {
  metodo?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  cuerpo?: unknown;
  /** Parámetros de consulta; los vacíos o nulos se omiten. */
  parametros?: Record<string, string | number | boolean | null | undefined>;
  /** false para endpoints públicos (login, transparencia). */
  autenticado?: boolean;
  signal?: AbortSignal;
}

function construirUrl(ruta: string, parametros?: Opciones['parametros']): string {
  const url = new URL(`${API_URL}${ruta}`);
  Object.entries(parametros ?? {}).forEach(([clave, valor]) => {
    if (valor !== undefined && valor !== null && valor !== '') url.searchParams.set(clave, String(valor));
  });
  return url.toString();
}

async function aError(respuesta: Response): Promise<ErrorApi> {
  try {
    const cuerpo = (await respuesta.json()) as Partial<ErrorRespuesta>;
    return new ErrorApi(respuesta.status, cuerpo.codigo ?? 'ERROR', cuerpo.mensaje ?? respuesta.statusText,
      cuerpo.detalles ?? []);
  } catch {
    return new ErrorApi(respuesta.status, 'ERROR', `Error ${respuesta.status}`);
  }
}

/**
 * Llama a la API con JSON. Si el access token venció (401), lo renueva una vez con el refresh token y
 * reintenta. Lanza {@link ErrorApi} con el formato uniforme del backend.
 */
export async function api<T>(ruta: string, opciones: Opciones = {}): Promise<T> {
  const { metodo = 'GET', cuerpo, parametros, autenticado = true, signal } = opciones;

  const enviar = () => {
    const headers: Record<string, string> = { Accept: 'application/json' };
    if (cuerpo !== undefined) headers['Content-Type'] = 'application/json';
    if (autenticado && accessToken) headers.Authorization = `Bearer ${accessToken}`;
    return fetch(construirUrl(ruta, parametros), {
      method: metodo,
      headers,
      body: cuerpo !== undefined ? JSON.stringify(cuerpo) : undefined,
      signal,
    });
  };

  let respuesta: Response;
  try {
    if (autenticado && !accessToken && hayRefreshGuardado()) await refrescarSesion();
    respuesta = await enviar();
    if (respuesta.status === 401 && autenticado && hayRefreshGuardado()) {
      if (await refrescarSesion()) respuesta = await enviar();
    }
  } catch (e) {
    if (e instanceof DOMException && e.name === 'AbortError') throw e;
    throw new ErrorApi(0, 'SIN_CONEXION',
      'No se pudo conectar con el servidor. Si es la primera visita del día, espera un minuto: el servidor se está iniciando.');
  }

  if (!respuesta.ok) throw await aError(respuesta);
  if (respuesta.status === 204) return undefined as T;
  return (await respuesta.json()) as T;
}
