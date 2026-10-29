import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { establecerSesion } from '../api/cliente';
import { ProveedorSesion } from '../auth/ContextoSesion';
import { RutaProtegida } from '../componentes/RutaProtegida';
import { Login } from './Login';

const json = (status: number, cuerpo: unknown) =>
  new Response(JSON.stringify(cuerpo), { status, headers: { 'Content-Type': 'application/json' } });

const respuestaLogin = (rol: string) => ({
  accessToken: 'A', refreshToken: 'R', tipoToken: 'Bearer', expiraEnSegundos: 900,
  usuario: { id: 1, nombres: 'Ana', apellidos: 'Pérez', rol },
});

function montar() {
  render(
    <QueryClientProvider client={new QueryClient()}>
      <MemoryRouter initialEntries={['/admin']}>
        <ProveedorSesion>
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route path="/admin" element={<RutaProtegida><p>Panel del admin</p></RutaProtegida>} />
          </Routes>
        </ProveedorSesion>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

async function ingresar(dni: string, password: string) {
  const usuario = userEvent.setup();
  await usuario.type(screen.getByLabelText('DNI'), dni);
  await usuario.type(screen.getByLabelText('Contraseña'), password);
  await usuario.click(screen.getByRole('button', { name: 'Ingresar' }));
}

describe('login del panel', () => {
  beforeEach(() => establecerSesion(null));

  it('sin sesión manda al login y el admin vuelve al panel', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(json(200, respuestaLogin('ADMIN')));
    montar();

    await ingresar('10000001', 'Admin2026!');

    expect(await screen.findByText('Panel del admin')).toBeInTheDocument();
    expect(JSON.parse(fetchMock.mock.calls[0]![1]!.body as string)).toEqual({ dni: '10000001', password: 'Admin2026!' });
  });

  it('un conductor o vecino no entra al panel web', async () => {
    vi.spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(json(200, respuestaLogin('VECINO')))
      .mockResolvedValueOnce(new Response(null, { status: 204 }));
    montar();

    await ingresar('30000001', 'Vecino2026!');

    expect(await screen.findByRole('alert')).toHaveTextContent('solo para administradores');
    expect(localStorage.getItem('aguaya.refreshToken')).toBeNull();
  });

  it('muestra el mensaje del backend si las credenciales fallan', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(json(401, {
      status: 401, codigo: 'CREDENCIALES_INVALIDAS', mensaje: 'DNI o contraseña incorrectos', detalles: [],
    }));
    montar();

    await ingresar('10000001', 'mala');

    expect(await screen.findByRole('alert')).toHaveTextContent('DNI o contraseña incorrectos');
  });
});
