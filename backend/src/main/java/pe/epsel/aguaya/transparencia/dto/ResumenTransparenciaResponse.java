package pe.epsel.aguaya.transparencia.dto;

import java.time.LocalDate;

/**
 * Indicadores generales del servicio en un periodo (RF09). Datos agregados, sin datos personales.
 *
 * @param sectoresAtendidos  sectores con al menos una ruta cerrada en el periodo
 * @param distritos          distritos con sectores activos
 * @param litrosDistribuidos litros cargados en los viajes del periodo
 */
public record ResumenTransparenciaResponse(
        LocalDate desde,
        LocalDate hasta,
        int sectoresActivos,
        int sectoresAtendidos,
        int distritos,
        Rutas rutas,
        Paradas paradas,
        long litrosDistribuidos,
        Calificacion calificacion,
        Incidencias incidencias,
        Flota flota) {

    public record Rutas(long total, long programadas, long enCurso, long completadas, long interrumpidas) {
    }

    /**
     * Paradas de rutas ya cerradas (completadas o interrumpidas).
     *
     * @param sinAtender             quedaron pendientes en una ruta interrumpida y no se reasignaron
     * @param cumplimientoPorcentaje atendidas ÷ total × 100; nulo si no hay paradas en el periodo
     */
    public record Paradas(long total, long atendidas, long noAtendidas, long sinAtender, Double cumplimientoPorcentaje) {
    }

    /** @param promedio nulo si no hay calificaciones */
    public record Calificacion(Double promedio, long total) {
    }

    /** @param resueltas reasignadas o cerradas por EPSEL */
    public record Incidencias(long reportadas, long resueltas) {
    }

    /** @param disponiblesHoy OPERATIVAS y con autorización sanitaria vigente hoy */
    public record Flota(long total, long disponiblesHoy) {
    }
}
