package pe.epsel.aguaya.transparencia.dto;

import java.time.LocalDate;

import pe.epsel.aguaya.common.dominio.Turno;
import pe.epsel.aguaya.ruta.entity.EstadoRuta;
import pe.epsel.aguaya.sector.entity.TipoEntrega;

/**
 * Una ruta en el cronograma público. La placa permite al vecino reconocer la cisterna asignada;
 * el conductor no se publica.
 */
public record CronogramaResponse(
        Long rutaId,
        LocalDate fecha,
        Turno turno,
        EstadoRuta estado,
        Long sectorId,
        String sectorNombre,
        String distrito,
        TipoEntrega tipoEntrega,
        String cisternaPlaca,
        long totalParadas,
        long paradasAtendidas) {
}
