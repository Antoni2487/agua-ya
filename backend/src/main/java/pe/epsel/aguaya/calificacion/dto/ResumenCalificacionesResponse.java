package pe.epsel.aguaya.calificacion.dto;

import java.util.List;
import java.util.Map;

/**
 * @param promedio     nulo si aún no hay calificaciones
 * @param distribucion cantidad por estrellas (1 a 5, siempre presentes)
 * @param conductores  del peor al mejor promedio, para detectar incumplimientos
 */
public record ResumenCalificacionesResponse(
        long total,
        Double promedio,
        Map<Integer, Long> distribucion,
        List<PorConductor> conductores) {

    public record PorConductor(Long conductorId, String nombre, double promedio, long total) {
    }
}
