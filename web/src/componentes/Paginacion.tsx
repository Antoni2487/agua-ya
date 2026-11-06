import type { Pagina } from '../api/tipos';
import { numero } from '../utilidades';

/** Navegación entre páginas de un listado del backend (page empieza en 0). */
export function Paginacion({ pagina, alCambiar }: { pagina: Pagina<unknown>; alCambiar: (page: number) => void }) {
  if (pagina.totalPages <= 1) return null;
  return (
    <nav className="paginacion" aria-label="Paginación">
      <button type="button" className="boton boton-secundario" disabled={pagina.page === 0} onClick={() => alCambiar(pagina.page - 1)}>
        Anterior
      </button>
      <span className="texto-suave">
        Página {pagina.page + 1} de {pagina.totalPages} · {numero(pagina.totalElements)} en total
      </span>
      <button
        type="button"
        className="boton boton-secundario"
        disabled={pagina.page + 1 >= pagina.totalPages}
        onClick={() => alCambiar(pagina.page + 1)}
      >
        Siguiente
      </button>
    </nav>
  );
}
