package pe.epsel.aguaya.incidencia.dto;

import jakarta.validation.constraints.NotNull;

/**
 * @param rutaDestinoId ruta que recibe las paradas pendientes; la elige el administrador
 */
public record ReasignarRequest(@NotNull Long rutaDestinoId) {
}
