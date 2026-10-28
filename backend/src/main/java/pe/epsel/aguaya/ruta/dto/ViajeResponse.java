package pe.epsel.aguaya.ruta.dto;

import java.time.OffsetDateTime;

import pe.epsel.aguaya.ruta.entity.EstadoViaje;

/**
 * @param paradasAtendidas paradas marcadas ATENDIDA durante este viaje
 */
public record ViajeResponse(
        Long id,
        Long rutaId,
        int nroViaje,
        EstadoViaje estado,
        Long puntoLlenadoId,
        String puntoLlenadoNombre,
        Integer volumenCargadoLitros,
        OffsetDateTime salida,
        OffsetDateTime retorno,
        long paradasAtendidas) {
}
