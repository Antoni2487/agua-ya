package pe.epsel.aguaya.monitoreo.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Recorrido real de una ruta para el mapa del administrador: posiciones GPS e intentos de
 * atención rechazados por estar fuera de la geocerca (evidencia ante desvíos).
 */
public record RecorridoResponse(
        Long rutaId,
        List<PosicionResponse> posiciones,
        List<IntentoFallido> intentosFallidos) {

    /**
     * @param distanciaMetros nula si el conductor no había enviado posición
     */
    public record IntentoFallido(
            Long paradaId,
            int ordenParada,
            BigDecimal latitud,
            BigDecimal longitud,
            BigDecimal distanciaMetros,
            OffsetDateTime registradoEn) {
    }
}
