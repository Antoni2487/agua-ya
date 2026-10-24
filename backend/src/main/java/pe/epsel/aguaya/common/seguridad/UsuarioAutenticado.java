package pe.epsel.aguaya.common.seguridad;

import pe.epsel.aguaya.persona.entity.Rol;

/**
 * Principal de Spring Security, construido desde el access token (sin consultar la BD).
 * En un controlador: {@code @AuthenticationPrincipal UsuarioAutenticado usuario}.
 */
public record UsuarioAutenticado(Long id, Rol rol) {
}
