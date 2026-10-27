package pe.epsel.aguaya.monitoreo.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Posición de la cisterna en una ruta. Es lo que se publica en {@code /topic/rutas/{id}/posicion}.
 */
public record PosicionResponse(
        Long rutaId,
        BigDecimal latitud,
        BigDecimal longitud,
        OffsetDateTime registradoEn) {
}
