import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import type { CronogramaItem, ResumenTransparencia, SectorTransparencia } from '../../api/tipos';
import { diaSemana, hoyEnLima, sumarDias } from '../../utilidades';
import { Transparencia } from './Transparencia';

// Leaflet necesita un navegador real; aquí basta saber qué sectores recibe el mapa.
vi.mock('../../componentes/MapaSectores', () => ({
  MapaSectores: ({ sectores }: { sectores: SectorTransparencia[] }) => (
    <div data-testid="mapa">{sectores.map((s) => s.nombre).join(',')}</div>
  ),
}));

const hoy = hoyEnLima();

const resumen: ResumenTransparencia = {
  desde: sumarDias(hoy, -29), hasta: hoy, sectoresActivos: 5, sectoresAtendidos: 2, distritos: 3,
  rutas: { total: 10, programadas: 3, enCurso: 1, completadas: 5, interrumpidas: 1 },
  paradas: { total: 40, atendidas: 36, noAtendidas: 3, sinAtender: 1, cumplimientoPorcentaje: 90 },
  litrosDistribuidos: 1_250_000,
  calificacion: { promedio: 4.3, total: 12 },
  incidencias: { reportadas: 4, resueltas: 3 },
  flota: { total: 5, disponiblesHoy: 3 },
};

const sector = (id: number, nombre: string, distrito: string, extra: Partial<SectorTransparencia> = {}): SectorTransparencia => ({
  sectorId: id, nombre, distrito, modalidad: 'PERMANENTE', esZonaAlta: true, tipoEntrega: 'PUNTO_ENTREGA',
  unidadesUso: 300, frecuenciaDias: 1, latCentro: -6.78, lngCentro: -79.82, ultimaAtencion: null,
  proximaAtencion: null, paradasAtendidas: 0, cumplimientoPorcentaje: null, calificacionPromedio: null, ...extra,
});

const sectores = [
  sector(1, 'Cerropón', 'Chiclayo', {
    proximaAtencion: { fecha: hoy, turno: 'MANANA', estado: 'EN_CURSO' }, cumplimientoPorcentaje: 87.5, calificacionPromedio: 4,
  }),
  sector(2, 'Callanca', 'Monsefú', { tipoEntrega: 'DOMICILIO', esZonaAlta: false }),
];

const cronograma: CronogramaItem[] = [
  { rutaId: 7, fecha: hoy, turno: 'MANANA', estado: 'EN_CURSO', sectorId: 1, sectorNombre: 'Cerropón', distrito: 'Chiclayo',
    tipoEntrega: 'PUNTO_ENTREGA', cisternaPlaca: 'AYA-101', totalParadas: 4, paradasAtendidas: 1 },
  { rutaId: 8, fecha: sumarDias(hoy, 2), turno: 'TARDE', estado: 'PROGRAMADA', sectorId: 2, sectorNombre: 'Callanca',
    distrito: 'Monsefú', tipoEntrega: 'DOMICILIO', cisternaPlaca: null, totalParadas: 6, paradasAtendidas: 0 },
];

function responder() {
  return vi.spyOn(globalThis, 'fetch').mockImplementation(async (entrada) => {
    const url = String(entrada);
    const cuerpo = url.includes('/resumen') ? resumen : url.includes('/sectores') ? sectores : cronograma;
    return new Response(JSON.stringify(cuerpo), { status: 200, headers: { 'Content-Type': 'application/json' } });
  });
}

function montar() {
  render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <MemoryRouter>
        <Transparencia />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('panel público de transparencia', () => {
  beforeEach(() => localStorage.clear());

  it('muestra cronograma, sectores e indicadores sin pedir login', async () => {
    const fetchMock = responder();
    montar();

    const deHoy = await screen.findByRole('region', { name: diaSemana(hoy) });
    expect(within(deHoy).getByText('Hoy')).toBeInTheDocument();
    expect(within(deHoy).getByText('AYA-101')).toBeInTheDocument();
    expect(within(deHoy).getByText('1/4 paradas')).toBeInTheDocument();

    expect(await screen.findByTestId('mapa')).toHaveTextContent('Cerropón,Callanca');
    expect(screen.getByText('87.5 %')).toBeInTheDocument();
    expect(await screen.findByText('90 %')).toBeInTheDocument();
    expect(screen.getByText(/1.250 m³/)).toBeInTheDocument();
    expect(screen.getByText('4.3 ★')).toBeInTheDocument();

    // Sin token: son endpoints públicos.
    fetchMock.mock.calls.forEach(([, init]) => {
      expect((init?.headers as Record<string, string>).Authorization).toBeUndefined();
    });
    expect(fetchMock.mock.calls.map(([u]) => new URL(String(u)).pathname)).toEqual(expect.arrayContaining([
      '/api/public/transparencia/resumen', '/api/public/transparencia/sectores', '/api/public/transparencia/cronograma',
    ]));
  });

  it('filtra por distrito el mapa, los sectores y el cronograma', async () => {
    responder();
    montar();
    await screen.findByTestId('mapa');

    await userEvent.selectOptions(screen.getByLabelText('Distrito'), 'Monsefú');

    expect(screen.getByTestId('mapa')).toHaveTextContent(/^Callanca$/);
    expect(screen.queryByText('AYA-101')).not.toBeInTheDocument();
    expect(screen.getAllByText('Callanca').length).toBeGreaterThan(0);
  });

  it('cambiar el periodo vuelve a pedir los indicadores', async () => {
    const fetchMock = responder();
    montar();
    await screen.findByText('90 %');

    await userEvent.selectOptions(screen.getByLabelText('Periodo de los indicadores'), '7');

    const resumenes = fetchMock.mock.calls.map(([u]) => new URL(String(u))).filter((u) => u.pathname.endsWith('/resumen'));
    expect(resumenes.at(-1)!.searchParams.get('desde')).toBe(sumarDias(hoy, -6));
  });
});
