package pe.epsel.aguaya.ruta.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

import pe.epsel.aguaya.common.dominio.Turno;

/**
 * @param puntoLlenadoId planta, surtidor o pozo donde carga la cisterna; desde ahí se encadena el recorrido
 */
public record ProgramarRutaRequest(
        @NotNull Long sectorId,
        @NotNull LocalDate fecha,
        @NotNull Turno turno,
        @NotNull Long puntoLlenadoId) {
}
