package pe.epsel.aguaya.auth.dto;

import pe.epsel.aguaya.usuario.dto.UsuarioResumen;

/**
 * @param expiraEnSegundos vigencia del access token
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tipoToken,
        long expiraEnSegundos,
        UsuarioResumen usuario) {
}
