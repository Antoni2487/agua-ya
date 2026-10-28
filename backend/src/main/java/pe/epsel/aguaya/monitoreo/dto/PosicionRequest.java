package pe.epsel.aguaya.monitoreo.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Posición del celular del conductor. Se aceptan más de 6 decimales; se guardan 6 (~10 cm).
 */
public record PosicionRequest(
        @NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal latitud,
        @NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal longitud) {
}
