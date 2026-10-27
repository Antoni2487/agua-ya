package pe.epsel.aguaya.sector.dto;

import java.math.BigDecimal;

public record PuntoEntregaResponse(
        Long id,
        Long sectorId,
        String sectorNombre,
        String nombre,
        String referencia,
        BigDecimal latitud,
        BigDecimal longitud,
        boolean activo) {
}
