import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import type { Incidencia, SugerenciasReasignacion } from '../../api/tipos';
import { renderizar, simularApi } from '../../pruebas/utilidades';
import { IncidenciaDetalle } from './IncidenciaDetalle';

const incidencia = (extra: Partial<Incidencia> = {}): Incidencia => ({
  id: 3, rutaId: 2, sectorNombre: 'P.J. Atusparias', estadoRuta: 'INTERRUMPIDA', paradaId: null, ordenParada: null,
  tipo: 'AVERIA', descripcion: 'Llanta pinchada', estado: 'ABIERTA', rutaDestinoId: null, reportadoPorId: 5,
  reportadoPorNombre: 'José Santisteban', reportadoPorRol: 'CONDUCTOR', creadoEn: '2026-10-29T13:40:00Z',
  resueltaPorNombre: null, resueltoEn: null, ...extra,
});

const sugerencias: SugerenciasReasignacion = {
  incidenciaId: 3, rutaOrigenId: 2, paradasPendientes: 4,
  rutas: [
    { rutaId: 9, fecha: '2026-10-30', turno: 'MANANA', estado: 'PROGRAMADA', cisternaPlaca: 'AYA-103', capacidadLitros: 15_000,
      conductorNombre: 'Rosa Díaz', distanciaKm: null, paradasPendientes: 5 },
  ],
};

const ver = (id = 3) => renderizar(<IncidenciaDetalle />, { url: `/admin/incidencias/${id}`, ruta: '/admin/incidencias/:id' });

describe('detalle de incidencia', () => {
  it('reasigna las paradas pendientes a la ruta que elige el admin', async () => {
    // Como el backend real: después de reasignar, la incidencia ya no está ABIERTA.
    let actual = incidencia();
    const llamadas = simularApi({
      'GET /api/admin/incidencias/3': () => actual,
      'GET /api/admin/incidencias/3/sugerencias-reasignacion': sugerencias,
      'POST /api/admin/incidencias/3/reasignar': () => {
        actual = incidencia({ estado: 'REASIGNADA', rutaDestinoId: 9, resueltaPorNombre: 'Admin EPSEL', resueltoEn: '2026-10-29T14:00:00Z' });
        return { incidencia: actual, rutaOrigenId: 2, rutaDestinoId: 9, paradasReasignadas: 3, paradasYaEnDestino: 1 };
      },
    });
    ver();

    expect(await screen.findByText('Avería · P.J. Atusparias')).toBeInTheDocument();
    expect(await screen.findByText(/4 paradas pendientes pasarán/)).toBeInTheDocument();
    const boton = screen.getByRole('button', { name: 'Reasignar paradas' });
    expect(boton).toBeDisabled(); // el sistema no elige: decide el admin

    await userEvent.click(screen.getByRole('radio', { name: /30\/10\/2026 · Mañana/ }));
    await userEvent.click(boton);

    expect(await screen.findByRole('status')).toHaveTextContent('3 paradas pasaron a la ruta destino (1 ya estaban en ella)');
    expect(llamadas.find((l) => l.metodo === 'POST')!.cuerpo).toEqual({ rutaDestinoId: 9 });
    expect(screen.getByRole('link', { name: 'Ver ruta destino' })).toHaveAttribute('href', '/admin/rutas/9');
    expect(screen.getByText('Reasignada')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Reasignar paradas' })).not.toBeInTheDocument();
  });

  it('sin rutas destino invita a programar una', async () => {
    simularApi({
      'GET /api/admin/incidencias/3': incidencia(),
      'GET /api/admin/incidencias/3/sugerencias-reasignacion': { ...sugerencias, rutas: [] },
    });
    ver();

    expect(await screen.findByText(/No hay otra ruta del sector/)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Programa una' })).toHaveAttribute('href', '/admin/rutas');
  });

  it('una queja de vecino sobre una ruta programada solo se cierra', async () => {
    const abierta = incidencia({ id: 4, tipo: 'NO_LLEGO', estadoRuta: 'PROGRAMADA', reportadoPorRol: 'VECINO' });
    const cerrada = { ...abierta, estado: 'CERRADA' as const, resueltaPorNombre: 'Admin EPSEL', resueltoEn: '2026-10-29T14:00:00Z' };
    let actual = abierta;
    const llamadas = simularApi({
      'GET /api/admin/incidencias/4': () => actual,
      'PATCH /api/admin/incidencias/4/cerrar': () => (actual = cerrada),
    });
    ver(4);

    await screen.findByText('No llegó · P.J. Atusparias');
    expect(screen.queryByText('Reasignar paradas pendientes')).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Cerrar como resuelta' }));

    expect(await screen.findByText('Cerrada')).toBeInTheDocument();
    expect(screen.getByText(/Admin EPSEL/)).toBeInTheDocument();
    expect(llamadas.some((l) => l.metodo === 'PATCH')).toBe(true);
  });
});
