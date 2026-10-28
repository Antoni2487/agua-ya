package pe.epsel.aguaya.monitoreo.dto;

import java.time.OffsetDateTime;

/**
 * Evento que el panel del administrador recibe en {@code /topic/admin/rutas}.
 *
 * @param paradaId solo en eventos de parada
 */
public record EventoRuta(
        Tipo tipo,
        Long rutaId,
        Long paradaId,
        OffsetDateTime registradoEn) {

    public enum Tipo {
        RUTA_INICIADA,
        PARADA_ATENDIDA,
        PARADA_NO_ATENDIDA,
        RUTA_COMPLETADA,
        RUTA_INTERRUMPIDA
    }
}
