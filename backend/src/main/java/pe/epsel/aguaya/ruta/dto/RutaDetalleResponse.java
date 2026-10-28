package pe.epsel.aguaya.ruta.dto;

import java.util.List;

/**
 * Ruta con su punto de llenado, sus viajes y sus paradas en orden de recorrido.
 */
public record RutaDetalleResponse(
        RutaResumenResponse ruta,
        Long puntoLlenadoId,
        String puntoLlenadoNombre,
        List<ViajeResponse> viajes,
        List<ParadaResponse> paradas) {
}
