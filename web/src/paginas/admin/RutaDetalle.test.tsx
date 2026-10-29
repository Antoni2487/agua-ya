import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import type { Parada, RutaDetalle as Detalle, SugerenciasAsignacion } from '../../api/tipos';
import { renderizar, simularApi } from '../../pruebas/utilidades';
import { RutaDetalle } from './RutaDetalle';

vi.mock('../../componentes/MapaParadas', () => ({
  MapaParadas: ({ paradas }: { paradas: Parada[] }) => <div data-testid="mapa">{paradas.length} paradas</div>,
}));

const parada = (id: number, orden: number, extra: Partial<Parada> = {}): Parada => ({
  id, orden, tipo: 'PUNTO_ENTREGA', prioridad: 'NORMAL', estado: 'PENDIENTE', nombre: `Punto ${orden}`, referencia: null,
  latitud: -6.78, longitud: -79.82, viajeId: null, ...extra,
});

const detalle = (estado: Detalle['ruta']['estado'], extra: Partial<Detalle['ruta']> = {}): Detalle => ({
  ruta: {
    id: 5, fechaProgramada: '2026-11-02', turno: 'MANANA', estado, sectorId: 1, sectorNombre: 'Cerropón',
    cisternaId: null, cisternaPlaca: null, conductorId: null, conductorNombre: null, viajesPlanificados: null,
    totalParadas: 3, paradasAtendidas: 0, ...extra,
  },
  puntoLlenadoId: 1,
  puntoLlenadoNombre: 'Planta EPSEL',
  viajes: [],
  paradas: [parada(10, 1, { tipo: 'INSTITUCION', prioridad: 'CRITICA', nombre: 'Posta Cerropón' }), parada(11, 2), parada(12, 3)],
});

const sugerencias: SugerenciasAsignacion = {
  rutaId: 5, litrosPorVisita: 28_000, maxViajesDiarios: 4,
  cisternas: [
    { id: 3, placa: 'AYA-103', capacidadLitros: 15_000, viajesNecesarios: 2, excedeMaximoDiario: false },
    { id: 1, placa: 'AYA-101', capacidadLitros: 12_000, viajesNecesarios: 3, excedeMaximoDiario: false },
  ],
  conductores: [
    { id: 21, nombres: 'Carlos', apellidos: 'Quispe', turno: 'MANANA', mismoTurno: true, distanciaKm: 1.2 },
    { id: 22, nombres: 'Rosa', apellidos: 'Díaz', turno: 'TARDE', mismoTurno: false, distanciaKm: null },
  ],
};

const catalogos = { 'GET /api/admin/puntos-llenado': { contenido: [], page: 0, size: 100, totalElements: 0, totalPages: 0 } };

describe('detalle de ruta', () => {
  it('asigna con la primera sugerencia y permite corregir los viajes', async () => {
    const llamadas = simularApi({
      ...catalogos,
      'GET /api/admin/rutas/5': detalle('PROGRAMADA'),
      'GET /api/admin/rutas/5/sugerencias-asignacion': sugerencias,
      'PATCH /api/admin/rutas/5/asignar': detalle('PROGRAMADA', { cisternaId: 3, cisternaPlaca: 'AYA-103', conductorId: 21, conductorNombre: 'Carlos Quispe', viajesPlanificados: 3 }),
    });
    renderizar(<RutaDetalle />, { url: '/admin/rutas/5', ruta: '/admin/rutas/:id' });

    expect(await screen.findByText('Posta Cerropón')).toBeInTheDocument();
    expect(screen.getByText('Crítica')).toBeInTheDocument();
    expect(await screen.findByText(/28,000 L por visita/)).toBeInTheDocument();
    expect(screen.getByLabelText('Cisterna')).toHaveValue('3');
    expect(screen.getByLabelText('Conductor')).toHaveValue('21');
    expect(screen.getByLabelText('Viajes planificados')).toHaveValue(2);

    await userEvent.clear(screen.getByLabelText('Viajes planificados'));
    await userEvent.type(screen.getByLabelText('Viajes planificados'), '3');
    await userEvent.click(screen.getByRole('button', { name: 'Confirmar asignación' }));

    expect(await screen.findByText('Asignación guardada.')).toBeInTheDocument();
    expect(llamadas.find((l) => l.metodo === 'PATCH')!.cuerpo).toEqual({ cisternaId: 3, conductorId: 21, viajesPlanificados: 3 });
    expect(await screen.findAllByText('AYA-103')).not.toHaveLength(0);
  });

  it('al cambiar de cisterna recalcula los viajes sugeridos', async () => {
    simularApi({
      ...catalogos,
      'GET /api/admin/rutas/5': detalle('PROGRAMADA'),
      'GET /api/admin/rutas/5/sugerencias-asignacion': sugerencias,
    });
    renderizar(<RutaDetalle />, { url: '/admin/rutas/5', ruta: '/admin/rutas/:id' });

    await screen.findByRole('option', { name: /AYA-101/ });
    await userEvent.selectOptions(screen.getByLabelText('Cisterna'), '1');

    expect(screen.getByLabelText('Viajes planificados')).toHaveValue(3);
  });

  it('una ruta en curso no se asigna ni se elimina', async () => {
    simularApi({
      ...catalogos,
      'GET /api/admin/rutas/5': detalle('EN_CURSO', { cisternaPlaca: 'AYA-101', conductorNombre: 'Carlos Quispe' }),
    });
    renderizar(<RutaDetalle />, { url: '/admin/rutas/5', ruta: '/admin/rutas/:id' });

    expect(await screen.findByText('En curso')).toBeInTheDocument();
    expect(screen.queryByRole('form', { name: 'Asignar cisterna y conductor' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Eliminar ruta' })).not.toBeInTheDocument();
  });

  it('elimina una ruta programada tras confirmar', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const llamadas = simularApi({
      ...catalogos,
      'GET /api/admin/rutas/5': detalle('PROGRAMADA'),
      'GET /api/admin/rutas/5/sugerencias-asignacion': sugerencias,
      'DELETE /api/admin/rutas/5': { status: 204 },
    });
    renderizar(<RutaDetalle />, { url: '/admin/rutas/5', ruta: '/admin/rutas/:id', otras: { '/admin/rutas': <p>Lista de rutas</p> } });

    await userEvent.click(await screen.findByRole('button', { name: 'Eliminar ruta' }));

    expect(await screen.findByText('Lista de rutas')).toBeInTheDocument();
    await waitFor(() => expect(llamadas.some((l) => l.metodo === 'DELETE')).toBe(true));
  });
});
