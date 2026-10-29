import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render } from '@testing-library/react';
import type { ReactNode } from 'react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { vi } from 'vitest';

type Respuesta = { status?: number; cuerpo?: unknown };
type Manejador = (url: URL, cuerpo: unknown) => Respuesta | unknown;

/**
 * Simula el backend: `{ 'GET /api/admin/rutas': ..., 'POST /api/admin/rutas': (url, cuerpo) => ... }`.
 * Devuelve las llamadas recibidas para verificar método, ruta y cuerpo.
 */
export function simularApi(rutas: Record<string, Manejador | unknown>) {
  const llamadas: { metodo: string; url: URL; cuerpo: unknown }[] = [];
  vi.spyOn(globalThis, 'fetch').mockImplementation(async (entrada, init) => {
    const url = new URL(String(entrada));
    const metodo = init?.method ?? 'GET';
    const cuerpo = init?.body ? JSON.parse(String(init.body)) : undefined;
    llamadas.push({ metodo, url, cuerpo });
    const clave = Object.keys(rutas).find((k) => {
      const [m, patron] = k.split(' ');
      return m === metodo && new RegExp(`^${patron!.replace(/:\w+/g, '[^/]+')}$`).test(url.pathname);
    });
    if (!clave) return new Response(JSON.stringify({ codigo: 'NO_SIMULADO', mensaje: `${metodo} ${url.pathname}` }), { status: 404 });
    const definicion = rutas[clave];
    const resultado = typeof definicion === 'function' ? (definicion as Manejador)(url, cuerpo) : definicion;
    const { status = 200, cuerpo: respuesta } =
      resultado && typeof resultado === 'object' && ('status' in resultado || 'cuerpo' in resultado)
        ? (resultado as Respuesta)
        : { cuerpo: resultado };
    if (status === 204) return new Response(null, { status });
    return new Response(JSON.stringify(respuesta), { status, headers: { 'Content-Type': 'application/json' } });
  });
  return llamadas;
}

/** Renderiza una pantalla con React Query y el router en la URL indicada. */
export function renderizar(elemento: ReactNode, { url = '/', ruta = '/', otras = {} as Record<string, ReactNode> } = {}) {
  const cliente = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={cliente}>
      <MemoryRouter initialEntries={[url]}>
        <Routes>
          <Route path={ruta} element={elemento} />
          {Object.entries(otras).map(([r, e]) => (
            <Route key={r} path={r} element={e} />
          ))}
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}
