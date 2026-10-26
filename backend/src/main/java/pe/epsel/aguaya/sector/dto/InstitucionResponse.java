package pe.epsel.aguaya.sector.dto;

import java.math.BigDecimal;

import pe.epsel.aguaya.sector.entity.TipoInstitucion;

public record InstitucionResponse(
        Long id,
        String nombre,
        TipoInstitucion tipo,
        Long sectorId,
        String sectorNombre,
        BigDecimal latitud,
        BigDecimal longitud,
        boolean activo) {
}
