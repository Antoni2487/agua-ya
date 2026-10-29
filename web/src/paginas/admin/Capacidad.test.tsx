import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import type { CapacidadDetalle } from '../../api/tipos';
import { renderizar, simularApi } from '../../pruebas/utilidades';
import { Capacidad } from './Capacidad';

const respuesta = (capacidad: number, deficit: number): CapacidadDetalle => ({
  fecha: '2026-10-29', capacidadReferenciaLitros: capacidad, cisternasNecesarias: 3 + deficit, cisternasDisponibles: 3,
  deficit, hayDeficit: deficit > 0, flota: { total: 5, disponibles: 3, noOperativas: 1, autorizacionVencida: 1 },
  sectores: [{
    sectorId: 3, nombre: 'P.J. Urrunaga', distrito: 'José Leonardo Ortiz', esZonaAlta: false, unidadesUso: 280, dotacionLitros: 80,
    frecuenciaDias: 2, litrosPorVisita: 44_800, viajesNecesarios: Math.ceil(44_800 / capacidad), maxViajesDiarios: 3,
    cisternasNecesarias: Math.ceil(Math.ceil(44_800 / capacidad) / 3), deficit: 0,
  }],
});

describe('planificación de capacidad', () => {
  it('muestra el déficit y permite simular cisternas más grandes', async () => {
    const llamadas = simularApi({
      'GET /api/admin/capacidad': (url: URL) =>
        url.searchParams.get('capacidadLitros') === '20000' ? respuesta(20_000, 0) : respuesta(11_333, 3),
    });
    renderizar(<Capacidad />, { url: '/admin/capacidad?fecha=2026-10-29', ruta: '/admin/capacidad' });

    expect(await screen.findByText('Faltan 3 cisternas el 29/10/2026 para atender todos los sectores activos.')).toBeInTheDocument();
    expect(screen.getByText('44,800 L')).toBeInTheDocument();

    await userEvent.type(screen.getByLabelText(/Simular capacidad/), '20000');
    await userEvent.click(screen.getByRole('button', { name: 'Simular' }));

    expect(await screen.findByText(/La flota alcanza.*Simulación con cisternas de 20,000 L/)).toBeInTheDocument();
    await waitFor(() => expect(llamadas.at(-1)!.url.searchParams.get('fecha')).toBe('2026-10-29'));
  });
});
