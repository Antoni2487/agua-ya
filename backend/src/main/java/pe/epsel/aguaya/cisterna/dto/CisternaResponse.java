package pe.epsel.aguaya.cisterna.dto;

import java.time.LocalDate;

import pe.epsel.aguaya.cisterna.entity.EstadoVehiculo;

/**
 * @param autorizacionVigente la autorización sanitaria sigue vigente hoy
 */
public record CisternaResponse(
        Long id,
        String placa,
        int capacidadLitros,
        EstadoVehiculo estadoVehiculo,
        String nroAutorizacionSanitaria,
        LocalDate venceAutorizacion,
        boolean autorizacionVigente) {
}
