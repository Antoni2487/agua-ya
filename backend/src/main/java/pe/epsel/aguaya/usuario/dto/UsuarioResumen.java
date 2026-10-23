package pe.epsel.aguaya.usuario.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import pe.epsel.aguaya.usuario.entity.EstadoVecino;
import pe.epsel.aguaya.usuario.entity.Rol;

/**
 * Datos básicos del usuario autenticado.
 *
 * @param estadoVecino solo para vecinos (la app muestra si su cuenta está pendiente de validación)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UsuarioResumen(
        Long id,
        String nombres,
        String apellidos,
        Rol rol,
        EstadoVecino estadoVecino) {
}
