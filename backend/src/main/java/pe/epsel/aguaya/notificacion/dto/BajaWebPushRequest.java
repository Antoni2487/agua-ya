package pe.epsel.aguaya.notificacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Baja de la suscripción del navegador (al cerrar sesión o al desactivar los avisos).
 */
public record BajaWebPushRequest(@NotBlank @Size(max = 1000) String endpoint) {
}
