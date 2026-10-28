package pe.epsel.aguaya.monitoreo.service;

import lombok.RequiredArgsConstructor;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import pe.epsel.aguaya.monitoreo.dto.EventoRuta;
import pe.epsel.aguaya.monitoreo.dto.PosicionResponse;

/**
 * Publica por STOMP. Si hay una transacción activa, envía después del commit para no
 * anunciar cambios que luego se revierten.
 */
@Component
@RequiredArgsConstructor
public class PublicadorTiempoReal {

    public static final String TOPICO_ADMIN_RUTAS = "/topic/admin/rutas";

    private final SimpMessagingTemplate messagingTemplate;

    public static String topicoPosicion(Long rutaId) {
        return "/topic/rutas/" + rutaId + "/posicion";
    }

    public void posicion(PosicionResponse posicion) {
        enviar(topicoPosicion(posicion.rutaId()), posicion);
    }

    public void eventoRuta(EventoRuta evento) {
        enviar(TOPICO_ADMIN_RUTAS, evento);
    }

    private void enviar(String destino, Object payload) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    messagingTemplate.convertAndSend(destino, payload);
                }
            });
        } else {
            messagingTemplate.convertAndSend(destino, payload);
        }
    }
}
