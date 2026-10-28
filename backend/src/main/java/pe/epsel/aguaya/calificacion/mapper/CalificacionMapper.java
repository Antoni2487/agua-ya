package pe.epsel.aguaya.calificacion.mapper;

import pe.epsel.aguaya.calificacion.dto.CalificacionResponse;
import pe.epsel.aguaya.calificacion.entity.CalificacionServicio;
import pe.epsel.aguaya.ruta.entity.Ruta;

public final class CalificacionMapper {

    private CalificacionMapper() {
    }

    public static CalificacionResponse aResponse(CalificacionServicio c) {
        Ruta r = c.getParada().getRuta();
        return new CalificacionResponse(c.getId(), c.getParada().getId(), r.getId(), r.getFechaProgramada(),
                r.getTurno(), r.getSector().getId(), r.getSector().getNombre(),
                r.getConductor() != null ? r.getConductor().getNombres() + " " + r.getConductor().getApellidos() : null,
                r.getCisterna() != null ? r.getCisterna().getPlaca() : null,
                c.getVecino().getNombres() + " " + c.getVecino().getApellidos(),
                c.getEstrellas(), c.getComentario(), c.getCreadoEn());
    }
}
