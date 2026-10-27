package pe.epsel.aguaya.ruta.dto;

import java.time.LocalDate;

import pe.epsel.aguaya.common.dominio.Turno;
import pe.epsel.aguaya.ruta.entity.EstadoRuta;

/**
 * Fila del listado de rutas. {@code totalParadas} y {@code paradasAtendidas} se calculan, no se guardan.
 *
 * @param cisternaId nulo mientras la ruta no esté asignada (igual conductor y viajes)
 */
public record RutaResumenResponse(
        Long id,
        LocalDate fechaProgramada,
        Turno turno,
        EstadoRuta estado,
        Long sectorId,
        String sectorNombre,
        Long cisternaId,
        String cisternaPlaca,
        Long conductorId,
        String conductorNombre,
        Integer viajesPlanificados,
        long totalParadas,
        long paradasAtendidas) {
}
