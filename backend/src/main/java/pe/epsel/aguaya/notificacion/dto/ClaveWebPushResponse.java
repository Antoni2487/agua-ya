package pe.epsel.aguaya.notificacion.dto;

/**
 * @param clavePublica clave VAPID pública (base64url) que el navegador pasa como {@code applicationServerKey};
 *                     nula si el servidor no tiene Web Push configurado
 */
public record ClaveWebPushResponse(String clavePublica) {
}
