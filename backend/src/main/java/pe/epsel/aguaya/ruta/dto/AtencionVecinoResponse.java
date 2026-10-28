package pe.epsel.aguaya.ruta.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;

import pe.epsel.aguaya.common.dominio.Turno;
import pe.epsel.aguaya.ruta.entity.EstadoParada;
import pe.epsel.aguaya.ruta.entity.EstadoRuta;
import pe.epsel.aguaya.ruta.entity.TipoParada;

/**
 * Lo que el vecino necesita saber de su atención: la ruta en curso o la próxima programada, y la última
 * realizada. Solo incluye su propia parada (su hogar o el punto de entrega más cercano), nunca la de otros.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AtencionVecinoResponse(Atencion proxima, Atencion ultima) {

    /**
     * @param miParada nula si el vecino se registró después de programar la ruta
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Atencion(
            Long rutaId,
            LocalDate fecha,
            Turno turno,
            EstadoRuta estado,
            long totalParadas,
            long paradasAtendidas,
            MiParada miParada) {
    }

    /**
     * @param pendientesAntes  paradas pendientes que la cisterna atenderá antes que la suya
     * @param miCalificacion   estrellas que ya dio a esta entrega; nula si aún no califica
     */
    public record MiParada(
            Long paradaId,
            TipoParada tipo,
            String nombre,
            String referencia,
            int orden,
            EstadoParada estado,
            long pendientesAntes,
            Integer miCalificacion) {
    }
}
