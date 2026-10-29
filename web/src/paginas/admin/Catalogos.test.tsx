import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import type { Cisterna, Conductor, Pagina, Sector } from '../../api/tipos';
import { renderizar, simularApi } from '../../pruebas/utilidades';
import { Catalogos } from './Catalogos';

vi.mock('../../componentes/SelectorUbicacion', () => ({
  SelectorUbicacion: ({ alElegir }: { alElegir: (lat: number, lng: number) => void }) => (
    <button type="button" onClick={() => alElegir(-6.7581, -79.8603)}>
      Marcar en el mapa
    </button>
  ),
}));

const pagina = <T,>(contenido: T[]): Pagina<T> => ({ contenido, page: 0, size: 20, totalElements: contenido.length, totalPages: 1 });

const sector: Sector = {
  id: 1, nombre: 'Cerropón', distrito: 'Chiclayo', modalidad: 'PERMANENTE', esZonaAlta: true, unidadesUso: 320,
  dotacionLitros: 80, frecuenciaDias: 2, maxViajesDiarios: 3, latCentro: -6.7581, lngCentro: -79.8603, tipoEntrega: 'DOMICILIO', activo: true,
};

const conductor: Conductor = {
  id: 7, dni: '20000001', nombres: 'Luis', apellidos: 'Chapoñán Vidaurre', telefono: '987654321', email: null,
  licencia: 'A-IIIb 4521', estadoDisponibilidad: 'EN_RUTA', turno: 'MANANA', activo: true,
};

const renderizarCatalogo = (tipo: string) =>
  renderizar(<Catalogos />, { url: `/admin/catalogos/${tipo}`, ruta: '/admin/catalogos/:tipo' });

describe('catálogos', () => {
  it('crea un sector con números, el centro marcado en el mapa y la dotación por defecto', async () => {
    const llamadas = simularApi({
      'GET /api/admin/sectores': pagina([sector]),
      'POST /api/admin/sectores': (_: URL, cuerpo: Record<string, unknown>) => ({ status: 201, cuerpo: { ...sector, ...cuerpo, id: 2 } }),
    });
    renderizarCatalogo('sectores');

    expect(await screen.findByRole('button', { name: 'Desactivar Cerropón' })).toBeInTheDocument();
    expect(screen.getByText('Cada 2 días')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Nuevo sector' }));
    const formulario = screen.getByRole('form', { name: 'Nuevo sector' });
    await userEvent.type(within(formulario).getByLabelText('Nombre'), 'A.H. San Nicolás');
    await userEvent.type(within(formulario).getByLabelText('Distrito'), 'Monsefú');
    await userEvent.selectOptions(within(formulario).getByLabelText(/Tipo de entrega/), 'PUNTO_ENTREGA');
    await userEvent.type(within(formulario).getByLabelText('Unidades de uso'), '150');
    await userEvent.type(within(formulario).getByLabelText(/Frecuencia/), '3');
    await userEvent.type(within(formulario).getByLabelText(/Máx\. viajes/), '2');
    await userEvent.click(await within(formulario).findByRole('button', { name: 'Marcar en el mapa' }));
    expect(within(formulario).getByLabelText('Latitud')).toHaveValue(-6.7581);
    await userEvent.click(within(formulario).getByLabelText(/Zona alta/));
    await userEvent.click(within(formulario).getByRole('button', { name: 'Guardar' }));

    expect(await screen.findByRole('status')).toHaveTextContent('Sector A.H. San Nicolás guardado.');
    expect(llamadas.find((l) => l.metodo === 'POST')!.cuerpo).toEqual({
      nombre: 'A.H. San Nicolás', distrito: 'Monsefú', modalidad: 'PERMANENTE', tipoEntrega: 'PUNTO_ENTREGA', unidadesUso: 150,
      dotacionLitros: null, frecuenciaDias: 3, maxViajesDiarios: 2, latCentro: -6.7581, lngCentro: -79.8603, esZonaAlta: true,
    });
    expect(screen.queryByRole('form')).not.toBeInTheDocument();
  });

  it('edita un conductor en ruta sin tocar su disponibilidad ni su contraseña', async () => {
    const llamadas = simularApi({
      'GET /api/admin/sectores': pagina([]),
      'GET /api/admin/conductores': pagina([conductor]),
      'PUT /api/admin/conductores/7': (_: URL, cuerpo: Record<string, unknown>) => ({ ...conductor, ...cuerpo, estadoDisponibilidad: 'EN_RUTA' }),
    });
    renderizarCatalogo('conductores');

    await userEvent.click(await screen.findByRole('button', { name: 'Editar Luis Chapoñán Vidaurre' }));
    const formulario = screen.getByRole('form', { name: 'Editar conductor' });
    expect(within(formulario).queryByLabelText('DNI')).not.toBeInTheDocument();
    expect(within(formulario).getByLabelText('Disponibilidad')).toHaveValue('');
    expect(within(formulario).getByLabelText(/^Contraseña/)).not.toBeRequired();
    await userEvent.selectOptions(within(formulario).getByLabelText('Turno'), 'TARDE');
    await userEvent.click(within(formulario).getByRole('button', { name: 'Guardar' }));

    expect(await screen.findByRole('status')).toHaveTextContent('Conductor Luis Chapoñán Vidaurre guardado.');
    expect(llamadas.find((l) => l.metodo === 'PUT')!.cuerpo).toEqual({
      nombres: 'Luis', apellidos: 'Chapoñán Vidaurre', telefono: '987654321', email: null, licencia: 'A-IIIb 4521',
      turno: 'TARDE', estadoDisponibilidad: null, password: null, activo: true,
    });
  });

  it('filtra por URL y muestra por qué no se pudo eliminar una cisterna', async () => {
    const cisterna: Cisterna = {
      id: 3, placa: 'T3K-482', capacidadLitros: 10_000, estadoVehiculo: 'OPERATIVA', nroAutorizacionSanitaria: 'DIRESA-0142',
      venceAutorizacion: '2026-05-31', autorizacionVigente: false,
    };
    const llamadas = simularApi({
      'GET /api/admin/sectores': pagina([]),
      'GET /api/admin/cisternas': pagina([cisterna]),
      'DELETE /api/admin/cisternas/3': {
        status: 409, cuerpo: { codigo: 'CISTERNA_CON_RUTAS', mensaje: 'La cisterna tiene rutas registradas; cambia su estado en lugar de eliminarla' },
      },
    });
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    renderizarCatalogo('cisternas');

    expect(await screen.findByText('Vencida')).toBeInTheDocument();
    await userEvent.selectOptions(screen.getByLabelText('Estado'), 'OPERATIVA');
    expect(llamadas.at(-1)!.url.searchParams.get('estado')).toBe('OPERATIVA');
    await userEvent.click(await screen.findByRole('button', { name: 'Eliminar T3K-482' }));

    expect(window.confirm).toHaveBeenCalledWith('¿Eliminar la cisterna T3K-482? Solo es posible si nunca tuvo rutas.');
    expect(await screen.findByRole('alert')).toHaveTextContent('cambia su estado en lugar de eliminarla');
  });
});
