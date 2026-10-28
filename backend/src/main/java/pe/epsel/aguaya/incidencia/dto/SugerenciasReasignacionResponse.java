package pe.epsel.aguaya.incidencia.dto;

import java.time.LocalDate;
import java.util.List;

import pe.epsel.aguaya.common.dominio.Turno;
import pe.epsel.aguaya.ruta.entity.EstadoRuta;

/**
 * Rutas del mismo sector a las que el administrador puede mover las paradas pendientes (RF07). El sistema
 * solo ordena: primero las cisternas en ruta más cercanas y luego por fecha y turno. El administrador elige.
 */
public record SugerenciasReasignacionResponse(
        Long incidenciaId,
        Long rutaOrigenId,
        long paradasPendientes,
        List<RutaCandidata> rutas) {

    /**
     * @param distanciaKm de la última posición del conductor a la primera parada pendiente; solo rutas EN_CURSO
     */
    public record RutaCandidata(
            Long rutaId,
            LocalDate fecha,
            Turno turno,
            EstadoRuta estado,
            String cisternaPlaca,
            Integer capacidadLitros,
            String conductorNombre,
            Double distanciaKm,
            long paradasPendientes) {
    }
}
