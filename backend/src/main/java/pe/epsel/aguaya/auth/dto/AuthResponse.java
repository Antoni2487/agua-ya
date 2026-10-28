package pe.epsel.aguaya.auth.dto;

import pe.epsel.aguaya.persona.dto.PersonaResumen;

/**
 * @param expiraEnSegundos vigencia del access token
 * @param usuario          la persona que inició sesión
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tipoToken,
        long expiraEnSegundos,
        PersonaResumen usuario) {
}
