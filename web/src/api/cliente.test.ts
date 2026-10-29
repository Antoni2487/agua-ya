import { beforeEach, describe, expect, it, vi } from 'vitest';

import { api, ErrorApi, establecerSesion, refrescarSesion } from './cliente';
import type { AuthResponse } from './tipos';

const sesion = (access: string, refresh: string): AuthResponse => ({
  accessToken: access,
  refreshToken: refresh,
  tipoToken: 'Bearer',
  expiraEnSegundos: 900,
  usuario: { id: 1, nombres: 'Ana', apellidos: 'Pérez', rol: 'ADMIN' },
});

const json = (status: number, cuerpo: unknown) =>
  new Response(JSON.stringify(cuerpo), { status, headers: { 'Content-Type': 'application/json' } });

describe('cliente de la API', () => {
  beforeEach(() => establecerSesion(null));

  it('envía el access token y devuelve el JSON', async () => {
    establecerSesion(sesion('A1', 'R1'));
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(json(200, { ok: true }));

    await expect(api('/api/admin/sectores', { parametros: { page: 0, distrito: '', activo: null } }))
      .resolves.toEqual({ ok: true });

    const [url, init] = fetchMock.mock.calls[0]!;
    expect(String(url)).toMatch(/\/api\/admin\/sectores\?page=0$/);
    expect((init!.headers as Record<string, string>).Authorization).toBe('Bearer A1');
  });

  it('con 401 renueva el token una vez y reintenta', async () => {
    establecerSesion(sesion('VENCIDO', 'R1'));
    const fetchMock = vi.spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(json(401, { codigo: 'NO_AUTENTICADO', mensaje: 'x', detalles: [] }))
      .mockResolvedValueOnce(json(200, sesion('A2', 'R2')))
      .mockResolvedValueOnce(json(200, { ok: true }));

    await expect(api('/api/admin/rutas')).resolves.toEqual({ ok: true });

    expect(String(fetchMock.mock.calls[1]![0])).toMatch(/\/api\/auth\/refresh$/);
    expect(JSON.parse(fetchMock.mock.calls[1]![1]!.body as string)).toEqual({ refreshToken: 'R1' });
    expect((fetchMock.mock.calls[2]![1]!.headers as Record<string, string>).Authorization).toBe('Bearer A2');
    expect(localStorage.getItem('aguaya.refreshToken')).toBe('R2');
  });

  it('si el refresh también falla cierra la sesión', async () => {
    establecerSesion(sesion('VENCIDO', 'R1'));
    vi.spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(json(401, { codigo: 'NO_AUTENTICADO', mensaje: 'Sesión vencida', detalles: [] }))
      .mockResolvedValueOnce(json(401, { codigo: 'REFRESH_INVALIDO', mensaje: 'x', detalles: [] }));

    await expect(api('/api/admin/rutas')).rejects.toMatchObject({ status: 401, codigo: 'NO_AUTENTICADO' });
    expect(localStorage.getItem('aguaya.refreshToken')).toBeNull();
  });

  it('varias peticiones vencidas comparten un solo refresh', async () => {
    establecerSesion(sesion('VENCIDO', 'R1'));
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async () => json(200, sesion('A2', 'R2')));

    await Promise.all([refrescarSesion(), refrescarSesion(), refrescarSesion()]);

    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it('traduce el error uniforme del backend a ErrorApi', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(json(409, {
      status: 409, codigo: 'RUTA_DUPLICADA', mensaje: 'Ya existe una ruta', detalles: [],
    }));

    const error = await api('/api/admin/rutas', { metodo: 'POST', cuerpo: {}, autenticado: false }).catch((e) => e);
    expect(error).toBeInstanceOf(ErrorApi);
    expect(error).toMatchObject({ status: 409, codigo: 'RUTA_DUPLICADA', message: 'Ya existe una ruta' });
  });

  it('sin conexión da un mensaje claro (Render despertando)', async () => {
    vi.spyOn(globalThis, 'fetch').mockRejectedValue(new TypeError('Failed to fetch'));

    await expect(api('/api/public/transparencia/resumen', { autenticado: false }))
      .rejects.toMatchObject({ codigo: 'SIN_CONEXION' });
  });
});
