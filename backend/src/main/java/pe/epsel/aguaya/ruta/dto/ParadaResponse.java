package pe.epsel.aguaya.ruta.dto;

import java.math.BigDecimal;

import pe.epsel.aguaya.ruta.entity.EstadoParada;
import pe.epsel.aguaya.ruta.entity.PrioridadParada;
import pe.epsel.aguaya.ruta.entity.TipoParada;

/**
 * @param nombre     vecino, institución o punto de entrega
 * @param referencia dirección de referencia del vecino o referencia del punto
 */
public record ParadaResponse(
        Long id,
        int orden,
        TipoParada tipo,
        PrioridadParada prioridad,
        EstadoParada estado,
        String nombre,
        String referencia,
        BigDecimal latitud,
        BigDecimal longitud) {
}
