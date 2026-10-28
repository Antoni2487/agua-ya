package pe.epsel.aguaya.notificacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import pe.epsel.aguaya.notificacion.entity.Plataforma;

/**
 * @param expoToken  el que devuelve {@code Notifications.getExpoPushTokenAsync()} en la app
 * @param plataforma opcional
 */
public record RegistrarDispositivoRequest(
        @NotBlank @Size(max = 255) @Pattern(regexp = PATRON_TOKEN,
                message = "debe ser un token de Expo (ExponentPushToken[...])") String expoToken,
        Plataforma plataforma) {

    static final String PATRON_TOKEN = "^Expo(nent)?PushToken\\[[^\\]]+\\]$";
}
