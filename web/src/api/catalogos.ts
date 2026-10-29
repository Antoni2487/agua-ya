import { useQuery } from '@tanstack/react-query';

import { api } from './cliente';
import type { Pagina, PuntoLlenado, Sector } from './tipos';

/** Catálogos que cambian poco: se reutilizan entre pantallas durante 5 minutos. */
const CINCO_MINUTOS = 5 * 60_000;

export function useSectoresActivos() {
  return useQuery({
    queryKey: ['catalogo', 'sectores', 'activos'],
    queryFn: ({ signal }) =>
      api<Pagina<Sector>>('/api/admin/sectores', { parametros: { activo: true, size: 100, sort: 'nombre' }, signal })
        .then((p) => p.contenido),
    staleTime: CINCO_MINUTOS,
  });
}

export function usePuntosLlenadoActivos() {
  return useQuery({
    queryKey: ['catalogo', 'puntos-llenado', 'activos'],
    queryFn: ({ signal }) =>
      api<Pagina<PuntoLlenado>>('/api/admin/puntos-llenado', { parametros: { activo: true, size: 100, sort: 'nombre' }, signal })
        .then((p) => p.contenido),
    staleTime: CINCO_MINUTOS,
  });
}

/** Todos los sectores, también los inactivos: para filtrar y editar catálogos que dependen de un sector. */
export function useSectores() {
  return useQuery({
    queryKey: ['catalogo', 'sectores', 'todos'],
    queryFn: ({ signal }) =>
      api<Pagina<Sector>>('/api/admin/sectores', { parametros: { size: 100, sort: 'nombre' }, signal }).then((p) => p.contenido),
    staleTime: CINCO_MINUTOS,
  });
}
