import { screen, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { renderizar, simularApi } from '../../pruebas/utilidades';
import { Calificaciones } from './Calificaciones';

describe('calificaciones', () => {
  it('muestra promedio, distribución, ranking de conductores y comentarios', async () => {
    simularApi({
      'GET /api/admin/sectores': { contenido: [], page: 0, size: 100, totalElements: 0, totalPages: 0 },
      'GET /api/admin/calificaciones/resumen': {
        total: 3, promedio: 3.67, distribucion: { 1: 0, 2: 1, 3: 0, 4: 1, 5: 1 },
        conductores: [
          { conductorId: 2, nombre: 'José Santisteban', promedio: 2, total: 1 },
          { conductorId: 1, nombre: 'Carlos Quispe', promedio: 4.5, total: 2 },
        ],
      },
      'GET /api/admin/calificaciones': {
        contenido: [{
          id: 1, paradaId: 10, rutaId: 1, fecha: '2026-10-29', turno: 'MANANA', sectorId: 1, sectorNombre: 'Cerropón',
          conductorNombre: 'Carlos Quispe', cisternaPlaca: 'AYA-101', vecinoNombre: 'Ana Torres', estrellas: 4,
          comentario: 'Llegó a tiempo', creadoEn: '2026-10-29T14:00:00Z',
        }],
        page: 0, size: 20, totalElements: 1, totalPages: 1,
      },
    });
    renderizar(<Calificaciones />, { url: '/admin/calificaciones', ruta: '/admin/calificaciones' });

    expect(await screen.findByText('3.7 ★')).toBeInTheDocument();
    const filas = within(await screen.findByText('Por conductor (del peor al mejor)').then((h) => h.parentElement!)).getAllByRole('row');
    expect(filas[0]).toHaveTextContent('José Santisteban2.0 ★');
    expect(await screen.findByText('Llegó a tiempo')).toBeInTheDocument();
    expect(screen.getByLabelText('4 de 5 estrellas')).toHaveTextContent('★★★★☆');
  });
});
