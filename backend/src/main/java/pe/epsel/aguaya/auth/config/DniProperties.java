package pe.epsel.aguaya.auth.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Consulta de nombres por DNI al registrar un vecino (prefijo {@code aguaya.dni}).
 *
 * @param provider        {@code local} (dev y tests: nombres de prueba) o {@code miapi} (miapi.cloud)
 * @param url             endpoint de miapi.cloud; el DNI se agrega al final
 * @param token           Bearer token de miapi.cloud; obligatorio con {@code miapi}
 * @param timeoutSegundos la API puede tardar más de 20 s en responder
 */
@Validated
@ConfigurationProperties(prefix = "aguaya.dni")
public record DniProperties(
        @NotBlank String provider,
        @NotBlank String url,
        String token,
        @Positive int timeoutSegundos) {
}
