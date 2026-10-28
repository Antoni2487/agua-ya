package pe.epsel.aguaya.notificacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Baja de un dispositivo (al cerrar sesión en la app).
 */
public record DispositivoRequest(@NotBlank @Size(max = 255) String expoToken) {
}
