package pe.epsel.aguaya.ruta.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * @param puntoLlenadoId       opcional: por defecto, el punto de llenado de la ruta
 * @param volumenCargadoLitros litros cargados; no puede superar la capacidad de la cisterna
 */
public record IniciarViajeRequest(
        Long puntoLlenadoId,
        @NotNull @Positive @Max(50_000) Integer volumenCargadoLitros) {
}
