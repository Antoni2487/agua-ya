import { act, screen, within } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import type { Pagina, Posicion, RutaResumen } from '../../api/tipos';
import type { RutaEnMapa } from '../../componentes/MapaEnVivo';
import { emitir, suscripciones } from '../../pruebas/tiempoRealSimulado';
import { renderizar, simularApi } from '../../pruebas/utilidades';
import { hoyEnLima } from '../../utilidades';
import { Monitoreo } from './Monitoreo';

vi.mock('../../api/tiempoReal', async () => (await import('../../pruebas/tiempoRealSimulado')).moduloTiempoRealSimulado);
vi.mock('../../componentes/MapaEnVivo', () => ({
  MapaEnVivo: ({ rutas }: { rutas: RutaEnMapa[] }) => (
    <ul data-testid="mapa">
      {rutas.map((r) => (
        <li key={r.ruta.id}>
          {r.ruta.cisternaPlaca}: {r.posiciones.length} posiciones, última {r.posiciones.at(-1)?.latitud ?? '—'}
          {r.sinSenal ? ' (sin señal)' : ''}
        </li>
      ))}
    </ul>
  ),
}));

const hoy = hoyEnLima();
const ahoraIso = (desfaseS = 0) => new Date(Date.now() - desfaseS * 1000).toISOString();
const ruta: RutaResumen = {
  id: 1, fechaProgramada: hoy, turno: 'MANANA', estado: 'EN_CURSO', sectorId: 1, sectorNombre: 'Cerropón',
  cisternaId: 1, cisternaPlaca: 'AYA-101', conductorId: 2, conductorNombre: 'Carlos Quispe', viajesPlanificados: 3,
  totalParadas: 4, paradasAtendidas: 1,
};
const pagina = (contenido: RutaResumen[]): Pagina<RutaResumen> => ({ contenido, page: 0, size: 100, totalElements: contenido.length, totalPages: 1 });
const pos = (latitud: number, registradoEn: string): Posicion => ({ rutaId: 1, latitud, longitud: -79.82, registradoEn });

describe('mapa en vivo', () => {
  beforeEach(() => suscripciones.clear());

  it('muestra las cisternas en ruta y mueve su posición al llegar el GPS', async () => {
    const llamadas = simularApi({
      'GET /api/admin/rutas': pagina([ruta]),
      'GET /api/admin/rutas/1/recorrido': { rutaId: 1, posiciones: [pos(-6.78, ahoraIso(30))], intentosFallidos: [] },
    });
    renderizar(<Monitoreo />, { url: '/admin/monitoreo', ruta: '/admin/monitoreo' });

    expect(await screen.findByText(/AYA-101: 1 posiciones, última -6.78/)).toBeInTheDocument();
    expect(screen.getByText('1/4 paradas atendidas')).toBeInTheDocument();
    expect(screen.getByText(/última posición hace 30 s/)).toBeInTheDocument();
    expect(new URL(llamadas[0]!.url).searchParams.get('estado')).toBe('EN_CURSO');

    act(() => emitir('/topic/rutas/1/posicion', pos(-6.781, ahoraIso())));

    expect(screen.getByText(/AYA-101: 2 posiciones, última -6.781/)).toBeInTheDocument();
  });

  it('marca sin señal si no llega GPS en 2 minutos', async () => {
    simularApi({
      'GET /api/admin/rutas': pagina([ruta]),
      'GET /api/admin/rutas/1/recorrido': { rutaId: 1, posiciones: [pos(-6.78, ahoraIso(300))], intentosFallidos: [] },
    });
    renderizar(<Monitoreo />, { url: '/admin/monitoreo', ruta: '/admin/monitoreo' });

    expect(await screen.findByText(/\(sin señal\)/)).toBeInTheDocument();
    expect(screen.getByText(/Sin señal · última posición hace 5 min/)).toBeInTheDocument();
  });

  it('registra los eventos de las rutas y recarga la lista', async () => {
    const llamadas = simularApi({
      'GET /api/admin/rutas': pagina([ruta]),
      'GET /api/admin/rutas/1/recorrido': { rutaId: 1, posiciones: [], intentosFallidos: [] },
    });
    renderizar(<Monitoreo />, { url: '/admin/monitoreo', ruta: '/admin/monitoreo' });
    await screen.findByText(/Aún sin posición GPS/);
    const antes = llamadas.filter((l) => l.url.pathname === '/api/admin/rutas').length;

    act(() => emitir('/topic/admin/rutas', { tipo: 'PARADA_ATENDIDA', rutaId: 1, paradaId: 10, viajeId: null, registradoEn: ahoraIso() }));

    expect(within(document.querySelector('.eventos') as HTMLElement).getByText(/atendió una parada/)).toBeInTheDocument();
    await vi.waitFor(() => expect(llamadas.filter((l) => l.url.pathname === '/api/admin/rutas').length).toBeGreaterThan(antes));
  });

  it('sin rutas en curso lo indica', async () => {
    simularApi({ 'GET /api/admin/rutas': pagina([]) });
    renderizar(<Monitoreo />, { url: '/admin/monitoreo', ruta: '/admin/monitoreo' });

    expect(await screen.findByText(/No hay cisternas en ruta ahora/)).toBeInTheDocument();
  });
});
