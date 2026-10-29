import { describe, expect, it } from 'vitest';

import { tokenVigente, urlWebSocket } from './tiempoReal';

const jwt = (exp: number) => `x.${btoa(JSON.stringify({ sub: '1', rol: 'ADMIN', exp })).replace(/=+$/, '')}.firma`;

describe('tiempo real', () => {
  it('arma la URL del WebSocket desde la del backend', () => {
    expect(urlWebSocket('http://localhost:8080')).toBe('ws://localhost:8080/ws');
    expect(urlWebSocket('https://aguaya-backend.onrender.com')).toBe('wss://aguaya-backend.onrender.com/ws');
  });

  it('detecta si el access token está por vencer', () => {
    const ahora = Date.parse('2026-10-29T12:00:00Z');
    expect(tokenVigente(jwt(ahora / 1000 + 600), 30, ahora)).toBe(true);
    expect(tokenVigente(jwt(ahora / 1000 + 10), 30, ahora)).toBe(false);
    expect(tokenVigente(null)).toBe(false);
    expect(tokenVigente('basura')).toBe(false);
  });
});
