package pe.epsel.aguaya.transparencia.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import pe.epsel.aguaya.common.dominio.Turno;
import pe.epsel.aguaya.ruta.entity.EstadoRuta;
import pe.epsel.aguaya.sector.entity.Modalidad;
import pe.epsel.aguaya.sector.entity.TipoEntrega;

/**
 * Un sector en el panel público. Las coordenadas son las del centro del sector, nunca de viviendas.
 *
 * @param ultimaAtencion         fecha de la última ruta cerrada
 * @param proximaAtencion        la próxima ruta programada o en curso; nula si no hay
 * @param cumplimientoPorcentaje del periodo consultado; nulo si no hubo rutas cerradas
 */
public record SectorTransparenciaResponse(
        Long sectorId,
        String nombre,
        String distrito,
        Modalidad modalidad,
        boolean esZonaAlta,
        TipoEntrega tipoEntrega,
        int unidadesUso,
        int frecuenciaDias,
        BigDecimal latCentro,
        BigDecimal lngCentro,
        LocalDate ultimaAtencion,
        ProximaAtencion proximaAtencion,
        long paradasAtendidas,
        Double cumplimientoPorcentaje,
        Double calificacionPromedio) {

    public record ProximaAtencion(LocalDate fecha, Turno turno, EstadoRuta estado) {
    }
}
