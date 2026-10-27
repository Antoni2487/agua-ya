package pe.epsel.aguaya.sector.mapper;

import pe.epsel.aguaya.sector.dto.PuntoEntregaResponse;
import pe.epsel.aguaya.sector.entity.PuntoEntrega;

public final class PuntoEntregaMapper {

    private PuntoEntregaMapper() {
    }

    public static PuntoEntregaResponse aResponse(PuntoEntrega p) {
        return new PuntoEntregaResponse(p.getId(), p.getSector().getId(), p.getSector().getNombre(), p.getNombre(),
                p.getReferencia(), p.getLatitud(), p.getLongitud(), p.isActivo());
    }
}
