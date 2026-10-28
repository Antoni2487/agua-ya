package pe.epsel.aguaya.incidencia.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import pe.epsel.aguaya.incidencia.entity.TipoIncidencia;

/**
 * @param paradaId    opcional: la parada afectada (debe ser de la ruta)
 * @param detieneRuta solo el conductor: {@code true} interrumpe la ruta EN_CURSO (p. ej. avería)
 */
public record ReportarIncidenciaRequest(
        @NotNull Long rutaId,
        Long paradaId,
        @NotNull TipoIncidencia tipo,
        @Size(max = 500) String descripcion,
        Boolean detieneRuta) {
}
