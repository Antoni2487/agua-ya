package pe.epsel.aguaya.notificacion.service;

import java.util.List;
import java.util.Set;

/**
 * Envía notificaciones Web Push a la web del vecino. Se elige con {@code aguaya.web-push.provider}:
 * {@code log} o {@code vapid}.
 */
public interface ProveedorWebPush {

    /**
     * Mejor esfuerzo: un fallo del servicio de push no debe afectar la operación de la ruta.
     *
     * @return endpoints que el servicio de push reporta como vencidos (404/410) para darlos de baja
     */
    Set<String> enviar(List<MensajeWebPush> mensajes);
}
