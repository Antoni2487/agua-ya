package pe.epsel.aguaya.puntollenado.dto;

import java.math.BigDecimal;

import pe.epsel.aguaya.puntollenado.entity.TipoPuntoLlenado;

public record PuntoLlenadoResponse(
        Long id,
        String nombre,
        TipoPuntoLlenado tipo,
        String direccion,
        BigDecimal latitud,
        BigDecimal longitud,
        boolean activo) {
}
