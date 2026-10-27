package pe.epsel.aguaya.ruta.mapper;

import java.util.List;

import pe.epsel.aguaya.persona.entity.Conductor;
import pe.epsel.aguaya.ruta.dto.ParadaResponse;
import pe.epsel.aguaya.ruta.dto.RutaDetalleResponse;
import pe.epsel.aguaya.ruta.dto.RutaResumenResponse;
import pe.epsel.aguaya.ruta.entity.EstadoParada;
import pe.epsel.aguaya.ruta.entity.Parada;
import pe.epsel.aguaya.ruta.entity.Ruta;

public final class RutaMapper {

    private RutaMapper() {
    }

    public static RutaResumenResponse aResumen(Ruta r, long totalParadas, long paradasAtendidas) {
        Conductor c = r.getConductor();
        return new RutaResumenResponse(r.getId(), r.getFechaProgramada(), r.getTurno(), r.getEstado(),
                r.getSector().getId(), r.getSector().getNombre(),
                r.getCisterna() != null ? r.getCisterna().getId() : null,
                r.getCisterna() != null ? r.getCisterna().getPlaca() : null,
                c != null ? c.getId() : null,
                c != null ? c.getNombres() + " " + c.getApellidos() : null,
                r.getViajesPlanificados(), totalParadas, paradasAtendidas);
    }

    public static RutaDetalleResponse aDetalle(Ruta r, List<Parada> paradas) {
        long atendidas = paradas.stream().filter(p -> p.getEstado() == EstadoParada.ATENDIDA).count();
        return new RutaDetalleResponse(aResumen(r, paradas.size(), atendidas),
                r.getPuntoLlenado() != null ? r.getPuntoLlenado().getId() : null,
                r.getPuntoLlenado() != null ? r.getPuntoLlenado().getNombre() : null,
                paradas.stream().map(RutaMapper::aParada).toList());
    }

    public static ParadaResponse aParada(Parada p) {
        String nombre;
        String referencia;
        switch (p.getTipo()) {
            case HOGAR -> {
                nombre = p.getVecino().getNombres() + " " + p.getVecino().getApellidos();
                referencia = p.getVecino().getDireccionReferencia();
            }
            case INSTITUCION -> {
                nombre = p.getInstitucion().getNombre();
                referencia = null;
            }
            case PUNTO_ENTREGA -> {
                nombre = p.getPuntoEntrega().getNombre();
                referencia = p.getPuntoEntrega().getReferencia();
            }
            default -> throw new IllegalStateException("Tipo de parada no soportado: " + p.getTipo());
        }
        return new ParadaResponse(p.getId(), p.getOrden(), p.getTipo(), p.getPrioridad(), p.getEstado(),
                nombre, referencia, p.getLatitud(), p.getLongitud());
    }
}
