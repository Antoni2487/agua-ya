package pe.epsel.aguaya.calificacion.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @param paradaId su parada atendida (su hogar o el punto de entrega más cercano)
 */
public record CalificarRequest(
        @NotNull Long paradaId,
        @NotNull @Min(1) @Max(5) Integer estrellas,
        @Size(max = 500) String comentario) {
}
