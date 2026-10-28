package pe.epsel.aguaya.persona.mapper;

import pe.epsel.aguaya.persona.dto.ConductorResponse;
import pe.epsel.aguaya.persona.entity.Conductor;

public final class ConductorMapper {

    private ConductorMapper() {
    }

    public static ConductorResponse aResponse(Conductor c) {
        return new ConductorResponse(c.getId(), c.getDni(), c.getNombres(), c.getApellidos(), c.getTelefono(),
                c.getEmail(), c.getLicencia(), c.getEstadoDisponibilidad(), c.getTurno(), c.isActivo());
    }
}
