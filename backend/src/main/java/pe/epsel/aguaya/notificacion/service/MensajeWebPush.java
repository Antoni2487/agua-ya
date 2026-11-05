package pe.epsel.aguaya.notificacion.service;

import java.util.Map;

/**
 * Una notificación para un navegador suscrito. {@code datos} viaja al service worker de la web para abrir
 * la pantalla adecuada al tocar el aviso.
 */
public record MensajeWebPush(String endpoint, String p256dh, String auth, String titulo, String cuerpo,
                             Map<String, Object> datos) {
}
