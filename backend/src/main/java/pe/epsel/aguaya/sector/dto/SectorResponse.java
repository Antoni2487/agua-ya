package pe.epsel.aguaya.sector.dto;

import java.math.BigDecimal;

import pe.epsel.aguaya.sector.entity.Modalidad;
import pe.epsel.aguaya.sector.entity.TipoEntrega;

public record SectorResponse(
        Long id,
        String nombre,
        String distrito,
        Modalidad modalidad,
        boolean esZonaAlta,
        int unidadesUso,
        int dotacionLitros,
        int frecuenciaDias,
        int maxViajesDiarios,
        BigDecimal latCentro,
        BigDecimal lngCentro,
        TipoEntrega tipoEntrega,
        boolean activo) {
}
