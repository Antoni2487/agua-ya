package pe.epsel.aguaya.sector.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import pe.epsel.aguaya.sector.entity.Modalidad;
import pe.epsel.aguaya.sector.entity.TipoEntrega;

/**
 * Alta y edición de sector.
 *
 * @param dotacionLitros litros por unidad de uso al día; si se omite, 80 (SUNASS)
 * @param tipoEntrega    si se omite, DOMICILIO al crear / se mantiene al editar
 * @param activo         si se omite, se crea activo / se mantiene el valor actual
 */
public record SectorRequest(
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Size(max = 60) String distrito,
        @NotNull Modalidad modalidad,
        boolean esZonaAlta,
        @NotNull @PositiveOrZero Integer unidadesUso,
        @Positive Integer dotacionLitros,
        @NotNull @Positive Integer frecuenciaDias,
        @NotNull @Positive Integer maxViajesDiarios,
        @NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal latCentro,
        @NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal lngCentro,
        TipoEntrega tipoEntrega,
        Boolean activo) {
}
