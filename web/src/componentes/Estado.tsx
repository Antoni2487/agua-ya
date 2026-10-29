import { ErrorApi } from '../api/cliente';

export function Cargando({ texto = 'Cargando…' }: { texto?: string }) {
  return (
    <div className="estado" role="status" aria-live="polite">
      <span className="spinner" aria-hidden="true" />
      {texto}
    </div>
  );
}

/** Muestra el mensaje del backend (ya viene en español) o uno genérico. */
export function MensajeError({ error, reintentar }: { error: unknown; reintentar?: () => void }) {
  const mensaje = error instanceof ErrorApi ? error.mensaje() : 'Ocurrió un error inesperado.';
  return (
    <div className="alerta alerta-error" role="alert">
      <span>{mensaje}</span>
      {reintentar && (
        <button type="button" className="boton boton-secundario" onClick={reintentar}>
          Reintentar
        </button>
      )}
    </div>
  );
}
