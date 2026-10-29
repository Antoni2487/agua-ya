package pe.epsel.aguaya.persona.dto;

import java.math.BigDecimal;

import pe.epsel.aguaya.sector.entity.TipoEntrega;

/**
 * Datos del propio vecino autenticado (solo los ve él).
 */
public record VecinoPerfilResponse(
        Long id,
        String dni,
        String nombres,
        String apellidos,
        String telefono,
        String direccionReferencia,
        BigDecimal latitud,
        BigDecimal longitud,
        Sector sector) {

    public record Sector(Long id, String nombre, String distrito, TipoEntrega tipoEntrega) {
    }
}
