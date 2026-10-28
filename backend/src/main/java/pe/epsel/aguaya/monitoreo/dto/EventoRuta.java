package pe.epsel.aguaya.monitoreo.dto;

import java.time.OffsetDateTime;

/**
 * Evento que el panel del administrador recibe en {@code /topic/admin/rutas}.
 *
 * @param paradaId solo en eventos de parada
 * @param viajeId  solo en eventos de viaje
 */
public record EventoRuta(
        Tipo tipo,
        Long rutaId,
        Long paradaId,
        Long viajeId,
        OffsetDateTime registradoEn) {

    public EventoRuta(Tipo tipo, Long rutaId, Long paradaId, OffsetDateTime registradoEn) {
        this(tipo, rutaId, paradaId, null, registradoEn);
    }

    public enum Tipo {
        RUTA_INICIADA,
        PARADA_ATENDIDA,
        PARADA_NO_ATENDIDA,
        VIAJE_INICIADO,
        VIAJE_FINALIZADO,
        RUTA_COMPLETADA,
        RUTA_INTERRUMPIDA
    }
}
