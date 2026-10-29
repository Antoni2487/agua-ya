import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import type { Pagina, RutaResumen } from '../../api/tipos';
import { renderizar, simularApi } from '../../pruebas/utilidades';
import { hoyEnLima, sumarDias } from '../../utilidades';
import { Rutas } from './Rutas';

const hoy = hoyEnLima();
const pagina = (contenido: RutaResumen[]): Pagina<RutaResumen> => ({ contenido, page: 0, size: 20, totalElements: contenido.length, totalPages: 1 });
const ruta: RutaResumen = {
  id: 7, fechaProgramada: hoy, turno: 'MANANA', estado: 'EN_CURSO', sectorId: 1, sectorNombre: 'Cerropón',
  cisternaId: 1, cisternaPlaca: 'AYA-101', conductorId: 2, conductorNombre: 'Carlos Quispe', viajesPlanificados: 3,
  totalParadas: 4, paradasAtendidas: 1,
};
const catalogos = {
  'GET /api/admin/sectores': { contenido: [{ id: 1, nombre: 'Cerropón', distrito: 'Chiclayo' }, { id: 4, nombre: 'Callanca', distrito: 'Monsefú' }], page: 0, size: 100, totalElements: 2, totalPages: 1 },
  'GET /api/admin/puntos-llenado': { contenido: [{ id: 1, nombre: 'Planta EPSEL' }], page: 0, size: 100, totalElements: 1, totalPages: 1 },
};

describe('rutas', () => {
  it('lista las rutas del día y filtra por estado y sector', async () => {
    const llamadas = simularApi({ ...catalogos, 'GET /api/admin/rutas': pagina([ruta]) });
    renderizar(<Rutas />, { url: '/admin/rutas', ruta: '/admin/rutas' });

    const fila = (await screen.findByRole('link', { name: 'Cerropón' })).closest('tr')!;
    expect(within(fila).getByText('AYA-101')).toBeInTheDocument();
    expect(within(fila).getByText('1/4')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Cerropón' })).toHaveAttribute('href', '/admin/rutas/7');

    await userEvent.selectOptions(screen.getByLabelText('Estado'), 'EN_CURSO');
    await userEvent.selectOptions(screen.getByLabelText('Sector'), '4');

    await waitFor(() => {
      const ultima = llamadas.filter((l) => l.url.pathname === '/api/admin/rutas').at(-1)!.url.searchParams;
      expect(ultima.get('fecha')).toBe(hoy);
      expect(ultima.get('estado')).toBe('EN_CURSO');
      expect(ultima.get('sectorId')).toBe('4');
    });
  });

  it('programa una ruta y abre su detalle', async () => {
    const llamadas = simularApi({
      ...catalogos,
      'GET /api/admin/rutas': pagina([]),
      'POST /api/admin/rutas': { status: 201, cuerpo: { ruta: { ...ruta, id: 99, estado: 'PROGRAMADA' }, paradas: [], viajes: [] } },
    });
    renderizar(<Rutas />, { url: '/admin/rutas', ruta: '/admin/rutas', otras: { '/admin/rutas/:id': <p>Detalle abierto</p> } });

    await userEvent.click(await screen.findByRole('button', { name: 'Programar ruta' }));
    const formulario = screen.getByRole('form', { name: 'Programar ruta' });
    await screen.findByRole('option', { name: 'Callanca · Monsefú' });
    await userEvent.selectOptions(within(formulario).getByLabelText('Sector'), '4');
    await userEvent.clear(within(formulario).getByLabelText('Fecha'));
    await userEvent.type(within(formulario).getByLabelText('Fecha'), sumarDias(hoy, 1));
    await userEvent.selectOptions(within(formulario).getByLabelText('Turno'), 'TARDE');
    await userEvent.click(within(formulario).getByRole('button', { name: 'Programar' }));

    expect(await screen.findByText('Detalle abierto')).toBeInTheDocument();
    // El único punto de llenado activo se elige solo.
    expect(llamadas.find((l) => l.metodo === 'POST')!.cuerpo).toEqual({
      sectorId: 4, fecha: sumarDias(hoy, 1), turno: 'TARDE', puntoLlenadoId: 1,
    });
  });

  it('muestra el error de negocio si la ruta ya existe', async () => {
    simularApi({
      ...catalogos,
      'GET /api/admin/rutas': pagina([]),
      'POST /api/admin/rutas': { status: 409, cuerpo: { codigo: 'RUTA_DUPLICADA', mensaje: 'Ya existe una ruta para ese sector, fecha y turno', detalles: [] } },
    });
    renderizar(<Rutas />, { url: '/admin/rutas', ruta: '/admin/rutas' });

    await userEvent.click(await screen.findByRole('button', { name: 'Programar ruta' }));
    await screen.findByRole('option', { name: 'Cerropón · Chiclayo' });
    const formulario = screen.getByRole('form', { name: 'Programar ruta' });
    await userEvent.selectOptions(within(formulario).getByLabelText('Sector'), '1');
    await userEvent.click(within(formulario).getByRole('button', { name: 'Programar' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Ya existe una ruta para ese sector, fecha y turno');
  });
});
