import { Client, type IMessage } from '@stomp/stompjs';
import { createContext, useContext, useEffect, useRef, useState, type ReactNode } from 'react';

import { API_URL, refrescarSesion, tokenActual } from './cliente';

export type EstadoConexion = 'conectando' | 'conectado' | 'desconectado';

interface TiempoReal {
  estado: EstadoConexion;
  cliente: Client | null;
}

const ContextoTiempoReal = createContext<TiempoReal>({ estado: 'desconectado', cliente: null });

/** http://host → ws://host/ws · https://host → wss://host/ws (RNF03: cifrado en producción). */
export function urlWebSocket(apiUrl: string = API_URL): string {
  return `${apiUrl.replace(/^http/, 'ws')}/ws`;
}

/** true si el JWT vence en más de `margenSegundos` (el servidor solo lo valida en el CONNECT). */
export function tokenVigente(token: string | null, margenSegundos = 30, ahora: number = Date.now()): boolean {
  if (!token) return false;
  try {
    const carga = JSON.parse(atob(token.split('.')[1]!.replace(/-/g, '+').replace(/_/g, '/'))) as { exp?: number };
    return carga.exp !== undefined && carga.exp * 1000 - ahora > margenSegundos * 1000;
  } catch {
    return false;
  }
}

/**
 * Conexión STOMP del panel (CLAUDE.md §10: `/ws`, Authorization en el CONNECT). Se reconecta sola cada
 * 5 s; antes de cada intento renueva el access token si venció.
 */
export function ProveedorTiempoReal({ children }: { children: ReactNode }) {
  const [estado, setEstado] = useState<EstadoConexion>('conectando');
  const [cliente, setCliente] = useState<Client | null>(null);

  useEffect(() => {
    const stomp = new Client({
      brokerURL: urlWebSocket(),
      reconnectDelay: 5000,
      heartbeatIncoming: 20000,
      heartbeatOutgoing: 20000,
      beforeConnect: async (c) => {
        setEstado('conectando');
        if (!tokenVigente(tokenActual())) await refrescarSesion();
        c.connectHeaders = { Authorization: `Bearer ${tokenActual() ?? ''}` };
      },
      onConnect: () => setEstado('conectado'),
      onWebSocketClose: () => setEstado('desconectado'),
      onStompError: () => setEstado('desconectado'),
    });
    stomp.activate();
    setCliente(stomp);
    return () => {
      void stomp.deactivate();
    };
  }, []);

  return <ContextoTiempoReal.Provider value={{ estado, cliente }}>{children}</ContextoTiempoReal.Provider>;
}

export function useEstadoTiempoReal(): EstadoConexion {
  return useContext(ContextoTiempoReal).estado;
}

/**
 * Se suscribe a un tópico mientras el componente está montado y la conexión activa; al reconectar
 * vuelve a suscribirse. `destino` null no suscribe.
 */
export function useSuscripcion<T>(destino: string | null, alRecibir: (mensaje: T) => void) {
  const { estado, cliente } = useContext(ContextoTiempoReal);
  const callback = useRef(alRecibir);
  callback.current = alRecibir;

  useEffect(() => {
    if (!destino || !cliente || estado !== 'conectado') return;
    const suscripcion = cliente.subscribe(destino, (m: IMessage) => callback.current(JSON.parse(m.body) as T));
    return () => {
      try {
        suscripcion.unsubscribe();
      } catch {
        // La conexión ya se cerró.
      }
    };
  }, [destino, cliente, estado]);
}
