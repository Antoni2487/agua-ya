package pe.epsel.aguaya.calificacion.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import pe.epsel.aguaya.common.dominio.Turno;

public record CalificacionResponse(
        Long id,
        Long paradaId,
        Long rutaId,
        LocalDate fecha,
        Turno turno,
        Long sectorId,
        String sectorNombre,
        String conductorNombre,
        String cisternaPlaca,
        String vecinoNombre,
        int estrellas,
        String comentario,
        OffsetDateTime creadoEn) {
}
