package pe.epsel.aguaya.notificacion.service;

import java.util.List;
import java.util.Set;

/**
 * Envía notificaciones push. Se elige con {@code aguaya.push.provider}: {@code log} o {@code expo}.
 */
public interface ProveedorPush {

    /**
     * Mejor esfuerzo: un fallo del servicio de push no debe afectar la operación de la ruta.
     *
     * @return tokens que ya no son válidos (app desinstalada) para darlos de baja
     */
    Set<String> enviar(List<MensajePush> mensajes);
}
