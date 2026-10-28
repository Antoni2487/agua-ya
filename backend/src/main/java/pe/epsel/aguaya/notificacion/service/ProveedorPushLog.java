package pe.epsel.aguaya.notificacion.service;

import java.util.List;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Desarrollo: no envía nada, solo escribe cada notificación en el log.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "aguaya.push.provider", havingValue = "log", matchIfMissing = true)
public class ProveedorPushLog implements ProveedorPush {

    @Override
    public Set<String> enviar(List<MensajePush> mensajes) {
        mensajes.forEach(m -> log.info("Push a {}: {} - {} {}", m.expoToken(), m.titulo(), m.cuerpo(), m.datos()));
        return Set.of();
    }
}
