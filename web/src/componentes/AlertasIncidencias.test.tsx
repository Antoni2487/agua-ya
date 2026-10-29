import { act, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import type { Incidencia } from '../api/tipos';
import { emitir, suscripciones } from '../pruebas/tiempoRealSimulado';
import { renderizar } from '../pruebas/utilidades';
import { AlertasIncidencias } from './AlertasIncidencias';

vi.mock('../api/tiempoReal', async () => (await import('../pruebas/tiempoRealSimulado')).moduloTiempoRealSimulado);

const incidencia: Incidencia = {
  id: 3, rutaId: 7, sectorNombre: 'Cerropón', estadoRuta: 'INTERRUMPIDA', paradaId: null, ordenParada: null,
  tipo: 'AVERIA', descripcion: 'Llanta pinchada', estado: 'ABIERTA', rutaDestinoId: null, reportadoPorId: 2,
  reportadoPorNombre: 'Carlos Quispe', reportadoPorRol: 'CONDUCTOR', creadoEn: new Date().toISOString(),
  resueltaPorNombre: null, resueltoEn: null,
};

describe('alertas de incidencias', () => {
  beforeEach(() => suscripciones.clear());

  it('muestra cada incidencia al instante con enlace a la ruta y se puede cerrar', async () => {
    renderizar(<AlertasIncidencias />);

    act(() => emitir('/topic/admin/incidencias', incidencia));

    const alerta = screen.getByRole('alert');
    expect(alerta).toHaveTextContent('Avería · Cerropón');
    expect(alerta).toHaveTextContent('Conductor · ruta interrumpida');
    expect(alerta).toHaveTextContent('“Llanta pinchada”');
    expect(screen.getByRole('link', { name: 'Ver ruta' })).toHaveAttribute('href', '/admin/rutas/7');

    await userEvent.click(screen.getByRole('button', { name: 'Cerrar alerta' }));
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('no repite la misma incidencia y muestra como máximo cuatro', () => {
    renderizar(<AlertasIncidencias />);

    act(() => {
      emitir('/topic/admin/incidencias', incidencia);
      emitir('/topic/admin/incidencias', incidencia);
      for (let id = 10; id < 15; id++) emitir('/topic/admin/incidencias', { ...incidencia, id, tipo: 'NO_LLEGO', reportadoPorRol: 'VECINO' });
    });

    expect(screen.getAllByRole('alert')).toHaveLength(4);
  });
});
