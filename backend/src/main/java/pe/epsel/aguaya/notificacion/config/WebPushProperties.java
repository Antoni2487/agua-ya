package pe.epsel.aguaya.notificacion.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Web Push para la web del vecino (prefijo {@code aguaya.web-push}).
 *
 * @param provider     {@code log} (dev: solo escribe en el log) o {@code vapid} (envía a los servicios de push)
 * @param clavePublica clave VAPID pública, base64url (65 bytes sin comprimir); el navegador la usa al suscribirse
 * @param clavePrivada clave VAPID privada, base64url (32 bytes); obligatoria con {@code vapid}
 * @param sujeto       contacto para los servicios de push ({@code mailto:} o {@code https:})
 * @param ttlSegundos  cuánto guarda el servicio de push un aviso si el celular está apagado
 */
@Validated
@ConfigurationProperties(prefix = "aguaya.web-push")
public record WebPushProperties(
        @NotBlank String provider,
        String clavePublica,
        String clavePrivada,
        @NotBlank String sujeto,
        @Positive int ttlSegundos) {
}
