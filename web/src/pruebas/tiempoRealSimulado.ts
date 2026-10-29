import { vi } from 'vitest';

/** Manejadores registrados por tópico; `emitir` simula un mensaje del servidor STOMP. */
export const suscripciones = new Map<string, (mensaje: unknown) => void>();

export function emitir(destino: string, mensaje: unknown) {
  const manejador = suscripciones.get(destino);
  if (!manejador) throw new Error(`Nadie suscrito a ${destino}`);
  manejador(mensaje);
}

export const moduloTiempoRealSimulado = {
  useEstadoTiempoReal: () => 'conectado',
  useSuscripcion: (destino: string | null, alRecibir: (m: unknown) => void) => {
    if (destino) suscripciones.set(destino, alRecibir);
  },
  ProveedorTiempoReal: ({ children }: { children: unknown }) => children,
  urlWebSocket: vi.fn(),
  tokenVigente: vi.fn(),
};
