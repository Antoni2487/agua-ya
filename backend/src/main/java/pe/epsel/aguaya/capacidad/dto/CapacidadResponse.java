package pe.epsel.aguaya.capacidad.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Planificación de capacidad (RF02) para una fecha: lo que piden los sectores frente a la flota disponible.
 *
 * @param capacidadReferenciaLitros     capacidad con la que se calculan los viajes: la indicada o el promedio de
 *                                      las cisternas disponibles
 * @param cisternasNecesarias           suma de las cisternas que necesitan los sectores listados
 * @param cisternasDisponibles          OPERATIVAS y con autorización sanitaria vigente en la fecha
 * @param deficit                       cisternas que faltan; 0 si la flota alcanza
 */
public record CapacidadResponse(
        LocalDate fecha,
        int capacidadReferenciaLitros,
        int cisternasNecesarias,
        int cisternasDisponibles,
        int deficit,
        boolean hayDeficit,
        Flota flota,
        List<SectorCapacidad> sectores) {

    /**
     * @param noOperativas        en MANTENIMIENTO o AVERIADA
     * @param autorizacionVencida OPERATIVAS pero con la autorización vencida en la fecha
     */
    public record Flota(int total, int disponibles, int noOperativas, int autorizacionVencida) {
    }

    /**
     * @param litrosPorVisita     unidades de uso × dotación × frecuencia
     * @param viajesNecesarios    ⌈litros por visita ÷ capacidad de referencia⌉
     * @param cisternasNecesarias ⌈viajes necesarios ÷ máximo de viajes diarios⌉
     * @param deficit             frente a toda la flota disponible, como si el sector fuera el único
     */
    public record SectorCapacidad(
            Long sectorId,
            String nombre,
            String distrito,
            boolean esZonaAlta,
            int unidadesUso,
            int dotacionLitros,
            int frecuenciaDias,
            long litrosPorVisita,
            int viajesNecesarios,
            int maxViajesDiarios,
            int cisternasNecesarias,
            int deficit) {
    }
}
