package pe.epsel.aguaya.monitoreo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * @param volumenEntregadoLitros opcional: litros entregados en la parada
 */
public record AtenderParadaRequest(
        @PositiveOrZero @Max(50_000) Integer volumenEntregadoLitros) {
}
