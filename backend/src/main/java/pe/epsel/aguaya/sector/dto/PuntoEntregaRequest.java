package pe.epsel.aguaya.sector.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @param referencia texto libre para ubicarlo ("frente a la losa deportiva")
 * @param activo     si se omite, se crea activo / se mantiene el valor actual
 */
public record PuntoEntregaRequest(
        @NotNull Long sectorId,
        @NotBlank @Size(max = 150) String nombre,
        @Size(max = 255) String referencia,
        @NotNull @DecimalMin("-90") @DecimalMax("90") @Digits(integer = 3, fraction = 6) BigDecimal latitud,
        @NotNull @DecimalMin("-180") @DecimalMax("180") @Digits(integer = 3, fraction = 6) BigDecimal longitud,
        Boolean activo) {
}
