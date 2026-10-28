package pe.epsel.aguaya.persona.dto;

import pe.epsel.aguaya.common.dominio.Turno;
import pe.epsel.aguaya.persona.entity.EstadoDisponibilidad;

public record ConductorResponse(
        Long id,
        String dni,
        String nombres,
        String apellidos,
        String telefono,
        String email,
        String licencia,
        EstadoDisponibilidad estadoDisponibilidad,
        Turno turno,
        boolean activo) {
}
