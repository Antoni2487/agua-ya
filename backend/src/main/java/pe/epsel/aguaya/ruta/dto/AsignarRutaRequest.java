package pe.epsel.aguaya.ruta.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * @param viajesPlanificados opcional: si se omite se usa el cálculo SUNASS para la cisterna elegida
 */
public record AsignarRutaRequest(
        @NotNull Long cisternaId,
        @NotNull Long conductorId,
        @Positive @Max(50) Integer viajesPlanificados) {
}
