package pe.epsel.aguaya.puntollenado.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import pe.epsel.aguaya.puntollenado.entity.TipoPuntoLlenado;

/**
 * @param activo si se omite, se crea activo / se mantiene el valor actual
 */
public record PuntoLlenadoRequest(
        @NotBlank @Size(max = 150) String nombre,
        @NotNull TipoPuntoLlenado tipo,
        @Size(max = 255) String direccion,
        @NotNull @DecimalMin("-90") @DecimalMax("90") @Digits(integer = 3, fraction = 6) BigDecimal latitud,
        @NotNull @DecimalMin("-180") @DecimalMax("180") @Digits(integer = 3, fraction = 6) BigDecimal longitud,
        Boolean activo) {
}
