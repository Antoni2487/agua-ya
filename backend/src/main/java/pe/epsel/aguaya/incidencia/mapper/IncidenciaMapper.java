package pe.epsel.aguaya.incidencia.mapper;

import pe.epsel.aguaya.incidencia.dto.IncidenciaResponse;
import pe.epsel.aguaya.incidencia.entity.Incidencia;
import pe.epsel.aguaya.persona.entity.Persona;

public final class IncidenciaMapper {

    private IncidenciaMapper() {
    }

    public static IncidenciaResponse aResponse(Incidencia i) {
        Persona reporta = i.getReportadoPor();
        return new IncidenciaResponse(i.getId(), i.getRuta().getId(), i.getRuta().getSector().getNombre(),
                i.getRuta().getEstado(),
                i.getParada() != null ? i.getParada().getId() : null,
                i.getParada() != null ? i.getParada().getOrden() : null,
                i.getTipo(), i.getDescripcion(), i.getEstado(),
                i.getRutaDestino() != null ? i.getRutaDestino().getId() : null,
                reporta.getId(), nombre(reporta), reporta.getRol(), i.getCreadoEn(),
                i.getResueltaPor() != null ? nombre(i.getResueltaPor()) : null, i.getResueltoEn());
    }

    private static String nombre(Persona p) {
        return p.getNombres() + " " + p.getApellidos();
    }
}
