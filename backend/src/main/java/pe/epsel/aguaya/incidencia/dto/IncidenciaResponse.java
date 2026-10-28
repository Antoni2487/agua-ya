package pe.epsel.aguaya.incidencia.dto;

import java.time.OffsetDateTime;

import pe.epsel.aguaya.incidencia.entity.EstadoIncidencia;
import pe.epsel.aguaya.incidencia.entity.TipoIncidencia;
import pe.epsel.aguaya.persona.entity.Rol;
import pe.epsel.aguaya.ruta.entity.EstadoRuta;

/**
 * Incidencia tal como la ve el administrador (y quien la reportó). También se publica en
 * {@code /topic/admin/incidencias}.
 */
public record IncidenciaResponse(
        Long id,
        Long rutaId,
        String sectorNombre,
        EstadoRuta estadoRuta,
        Long paradaId,
        Integer ordenParada,
        TipoIncidencia tipo,
        String descripcion,
        EstadoIncidencia estado,
        Long rutaDestinoId,
        Long reportadoPorId,
        String reportadoPorNombre,
        Rol reportadoPorRol,
        OffsetDateTime creadoEn,
        String resueltaPorNombre,
        OffsetDateTime resueltoEn) {
}
