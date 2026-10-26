package pe.epsel.aguaya.sector.mapper;

import pe.epsel.aguaya.sector.dto.InstitucionResponse;
import pe.epsel.aguaya.sector.entity.InstitucionCritica;

public final class InstitucionMapper {

    private InstitucionMapper() {
    }

    public static InstitucionResponse aResponse(InstitucionCritica i) {
        return new InstitucionResponse(i.getId(), i.getNombre(), i.getTipo(), i.getSector().getId(),
                i.getSector().getNombre(), i.getLatitud(), i.getLongitud(), i.isActivo());
    }
}
