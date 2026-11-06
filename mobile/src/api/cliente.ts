import * as SecureStore from 'expo-secure-store';
import type { AuthResponse, ErrorRespuesta } from './tipos';

/** URL del backend (EXPO_PUBLIC_API_URL); por defecto el de Render (rama develop). */
export const API_URL = (process.env.EXPO_PUBLIC_API_URL ?? 'https://aguaya-backend.onrender.com').replace(/\/$/, '');

const CLAVE_SESION = 'aguaya.sesion';

/** Error de negocio o de validación del backend, con su código (p. ej. FUERA_DE_GEOCERCA). */
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

/** Texto para mostrar de cualquier error. */
export function textoError(e: unknown): string {
  if (e instanceof ErrorApi) return e.mensaje();
  if (e instanceof Error) return e.message;
  return 'Algo salió mal. Inténtalo otra vez.';
}

// La sesión completa (access + refresh + usuario) se guarda cifrada en el celular para no pedir el
// ingreso cada vez que se abre la app. El refresh token se rota en cada uso.
let sesionActual: AuthResponse | null = null;
let refrescando: Promise<AuthResponse | null> | null = null;
const oyentes = new Set<(sesion: AuthResponse | null) => void>();

export async function cargarSesionGuardada(): Promise<AuthResponse | null> {
  try {
    const texto = await SecureStore.getItemAsync(CLAVE_SESION);
    sesionActual = texto ? (JSON.parse(texto) as AuthResponse) : null;
  } catch {
    sesionActual = null;
  }
  return sesionActual;
}

export async function establecerSesion(sesion: AuthResponse | null): Promise<void> {
  sesionActual = sesion;
  try {
    if (sesion) await SecureStore.setItemAsync(CLAVE_SESION, JSON.stringify(sesion));
    else await SecureStore.deleteItemAsync(CLAVE_SESION);
  } catch {
    // Sin almacenamiento seguro: la sesión dura mientras la app está abierta.
  }
  oyentes.forEach((oyente) => oyente(sesion));
}

export function sesion(): AuthResponse | null {
  return sesionActual;
}

/** Avisa cuando la sesión cambia (ingreso, refresh, salida o refresh vencido). */
export function alCambiarSesion(oyente: (sesion: AuthResponse | null) => void): () => void {
  oyentes.add(oyente);
  return () => {
    oyentes.delete(oyente);
  };
}

/** Pide un access token nuevo con el refresh token. Una sola petición aunque fallen varias a la vez. */
function refrescarSesion(): Promise<AuthResponse | null> {
  const refreshToken = sesionActual?.refreshToken;
  if (!refreshToken) return Promise.resolve(null);
  refrescando ??= fetch(`${API_URL}/api/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify({ refreshToken }),
  })
    .then(async (r) => {
      const nueva = r.ok ? ((await r.json()) as AuthResponse) : null;
      await establecerSesion(nueva);
      return nueva;
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
  /** false para endpoints públicos (ingreso, registro). */
  autenticado?: boolean;
}

async function aError(respuesta: Response): Promise<ErrorApi> {
  try {
    const cuerpo = (await respuesta.json()) as Partial<ErrorRespuesta>;
    return new ErrorApi(respuesta.status, cuerpo.codigo ?? 'ERROR', cuerpo.mensaje ?? `Error ${respuesta.status}`,
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
  const { metodo = 'GET', cuerpo, autenticado = true } = opciones;
  const enviar = () => {
    const headers: Record<string, string> = { Accept: 'application/json' };
    if (cuerpo !== undefined) headers['Content-Type'] = 'application/json';
    if (autenticado && sesionActual) headers.Authorization = `Bearer ${sesionActual.accessToken}`;
    return fetch(`${API_URL}${ruta}`, {
      method: metodo,
      headers,
      body: cuerpo !== undefined ? JSON.stringify(cuerpo) : undefined,
    });
  };

  let respuesta: Response;
  try {
    respuesta = await enviar();
    if (respuesta.status === 401 && autenticado && sesionActual) {
      if (await refrescarSesion()) respuesta = await enviar();
    }
  } catch {
    throw new ErrorApi(0, 'SIN_CONEXION',
      'No hay conexión con el servidor. Revisa tus datos o wifi; si es la primera vez del día, espera un minuto mientras el servidor se inicia.');
  }

  if (!respuesta.ok) throw await aError(respuesta);
  if (respuesta.status === 204) return undefined as T;
  return (await respuesta.json()) as T;
}
