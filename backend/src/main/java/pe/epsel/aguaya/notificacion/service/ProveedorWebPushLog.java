package pe.epsel.aguaya.notificacion.service;

import java.util.List;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Desarrollo: no envía nada, solo escribe cada notificación Web Push en el log.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "aguaya.web-push.provider", havingValue = "log", matchIfMissing = true)
public class ProveedorWebPushLog implements ProveedorWebPush {

    @Override
    public Set<String> enviar(List<MensajeWebPush> mensajes) {
        mensajes.forEach(m -> log.info("Web Push a {}: {} - {} {}", m.endpoint(), m.titulo(), m.cuerpo(), m.datos()));
        return Set.of();
    }
}
