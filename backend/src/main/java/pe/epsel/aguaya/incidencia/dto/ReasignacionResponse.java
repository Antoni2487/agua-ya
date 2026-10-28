package pe.epsel.aguaya.incidencia.dto;

/**
 * @param paradasReasignadas paradas copiadas a la ruta destino
 * @param paradasYaEnDestino pendientes que la ruta destino ya tenía (el mismo hogar o punto): no se duplican
 */
public record ReasignacionResponse(
        IncidenciaResponse incidencia,
        Long rutaOrigenId,
        Long rutaDestinoId,
        int paradasReasignadas,
        int paradasYaEnDestino) {
}
