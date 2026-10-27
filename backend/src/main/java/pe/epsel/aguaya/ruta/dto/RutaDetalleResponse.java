package pe.epsel.aguaya.ruta.dto;

import java.util.List;

/**
 * Ruta con su punto de llenado y sus paradas en orden de recorrido.
 */
public record RutaDetalleResponse(
        RutaResumenResponse ruta,
        Long puntoLlenadoId,
        String puntoLlenadoNombre,
        List<ParadaResponse> paradas) {
}
