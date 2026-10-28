package pe.epsel.aguaya.common.exception;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;

/**
 * Formato uniforme de error de la API.
 *
 * @param detalles errores por campo (validación); lista vacía si no aplica
 */
public record ErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String codigo,
        String mensaje,
        List<DetalleError> detalles) {

    public record DetalleError(String campo, String mensaje) {
    }

    public static ErrorResponse de(HttpStatus status, String codigo, String mensaje) {
        return de(status, codigo, mensaje, List.of());
    }

    public static ErrorResponse de(HttpStatus status, String codigo, String mensaje, List<DetalleError> detalles) {
        return new ErrorResponse(OffsetDateTime.now(), status.value(), codigo, mensaje, detalles);
    }
}
