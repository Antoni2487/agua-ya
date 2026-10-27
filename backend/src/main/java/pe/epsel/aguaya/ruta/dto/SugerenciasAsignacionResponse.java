package pe.epsel.aguaya.ruta.dto;

import java.util.List;

import pe.epsel.aguaya.common.dominio.Turno;

/**
 * Candidatos para asignar a una ruta (RF03). El sistema sugiere; el administrador confirma.
 *
 * @param litrosPorVisita  unidades de uso × dotación × frecuencia del sector
 * @param maxViajesDiarios máximo de viajes del sector; una cisterna que lo supera marca déficit
 */
public record SugerenciasAsignacionResponse(
        Long rutaId,
        long litrosPorVisita,
        int maxViajesDiarios,
        List<CisternaSugerida> cisternas,
        List<ConductorSugerido> conductores) {

    /**
     * @param excedeMaximoDiario los viajes necesarios superan el máximo diario del sector
     */
    public record CisternaSugerida(
            Long id,
            String placa,
            int capacidadLitros,
            int viajesNecesarios,
            boolean excedeMaximoDiario) {
    }

    /**
     * @param distanciaKm de su última posición GPS al centro del sector; nula si aún no envió posición
     */
    public record ConductorSugerido(
            Long id,
            String nombres,
            String apellidos,
            Turno turno,
            boolean mismoTurno,
            Double distanciaKm) {
    }
}
