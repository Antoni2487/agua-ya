package pe.epsel.aguaya.notificacion.service;

import java.util.Map;

/**
 * Una notificación para un celular. {@code datos} viaja a la app para abrir la pantalla adecuada.
 */
public record MensajePush(String expoToken, String titulo, String cuerpo, Map<String, Object> datos) {
}
