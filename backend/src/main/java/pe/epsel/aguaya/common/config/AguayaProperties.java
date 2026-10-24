package pe.epsel.aguaya.common.config;

import java.time.ZoneId;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Propiedades propias de AguaYa (prefijo {@code aguaya}).
 */
@Validated
@ConfigurationProperties(prefix = "aguaya")
public record AguayaProperties(
        @NotNull ZoneId zonaHoraria,
        @Valid @NotNull Cors cors,
        @Valid @NotNull Geocerca geocerca,
        @Valid @NotNull Jwt jwt,
        @Valid @NotNull Otp otp,
        @NotNull Twilio twilio) {

    public record Cors(@NotEmpty List<String> origins) {
    }

    public record Geocerca(@Positive int radioMetros) {
    }

    /**
     * @param secret clave HMAC-SHA256; al menos 32 caracteres
     */
    public record Jwt(
            @NotBlank @Size(min = 32) String secret,
            @Positive int accessMinutos,
            @Positive int refreshDias) {
    }

    /**
     * @param provider        {@code log} (dev) o {@code twilio} (prod)
     * @param exponerCodigo   devuelve el código en la respuesta; solo para desarrollo
     * @param vigenciaMinutos solo proveedor {@code log}; con Twilio aplica su configuración
     * @param maxIntentos     solo proveedor {@code log}
     * @param maxEnviosHora   solo proveedor {@code log}
     */
    public record Otp(
            @NotBlank String provider,
            boolean exponerCodigo,
            @Positive int vigenciaMinutos,
            @Positive int maxIntentos,
            @Positive int maxEnviosHora) {
    }

    /**
     * Credenciales de Twilio Verify; obligatorias solo con {@code aguaya.otp.provider=twilio}.
     */
    public record Twilio(String accountSid, String authToken, String verifySid) {
    }
}
