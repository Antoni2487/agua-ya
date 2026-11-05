package pe.epsel.aguaya.notificacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Suscripción del navegador tal como la entrega {@code PushSubscription.toJSON()}:
 * {@code {"endpoint":"https://…","keys":{"p256dh":"…","auth":"…"}}} ({@code expirationTime} se ignora).
 */
public record SuscripcionWebPushRequest(
        @NotBlank @Size(max = 1000) String endpoint,
        @JsonProperty("keys") @NotNull @Valid Claves claves) {

    /**
     * @param p256dh clave pública del navegador (base64url)
     * @param auth   secreto de autenticación (base64url)
     */
    public record Claves(@NotBlank @Size(max = 100) String p256dh, @NotBlank @Size(max = 50) String auth) {
    }
}
