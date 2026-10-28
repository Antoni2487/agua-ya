package pe.epsel.aguaya.notificacion.service;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import pe.epsel.aguaya.common.config.AguayaProperties;

/**
 * Expo Push API por HTTP (sin SDK): https://docs.expo.dev/push-notifications/sending-notifications/.
 * Envía en lotes de 100 (límite de Expo) y devuelve los tokens que Expo marca como DeviceNotRegistered.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "aguaya.push.provider", havingValue = "expo")
public class ProveedorPushExpo implements ProveedorPush {

    static final String URL = "https://exp.host/--/api/v2/push/send";
    static final int LOTE = 100;

    private final RestClient restClient;

    public ProveedorPushExpo(RestClient.Builder builder, AguayaProperties properties) {
        String token = properties.push().expoAccessToken();
        this.restClient = builder.baseUrl(URL)
                .defaultHeaders(h -> {
                    h.setContentType(MediaType.APPLICATION_JSON);
                    h.setAccept(List.of(MediaType.APPLICATION_JSON));
                    if (StringUtils.hasText(token)) {
                        h.setBearerAuth(token);
                    }
                })
                .build();
    }

    @Override
    public Set<String> enviar(List<MensajePush> mensajes) {
        Set<String> invalidos = new HashSet<>();
        for (int i = 0; i < mensajes.size(); i += LOTE) {
            List<MensajePush> lote = mensajes.subList(i, Math.min(i + LOTE, mensajes.size()));
            try {
                Map<?, ?> respuesta = restClient.post()
                        .header(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate")
                        .body(lote.stream().map(ProveedorPushExpo::aJson).toList())
                        .retrieve()
                        .body(Map.class);
                invalidos.addAll(tokensNoRegistrados(lote, respuesta));
            } catch (RestClientException e) {
                log.error("Error al enviar {} notificaciones por Expo Push", lote.size(), e);
            }
        }
        return invalidos;
    }

    private static Map<String, Object> aJson(MensajePush m) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("to", m.expoToken());
        json.put("title", m.titulo());
        json.put("body", m.cuerpo());
        json.put("data", m.datos());
        json.put("sound", "default");
        json.put("priority", "high");
        return json;
    }

    /** Los tickets vuelven en el mismo orden que los mensajes. */
    private static Set<String> tokensNoRegistrados(List<MensajePush> lote, Map<?, ?> respuesta) {
        Set<String> invalidos = new HashSet<>();
        if (respuesta == null || !(respuesta.get("data") instanceof List<?> tickets)) {
            return invalidos;
        }
        for (int i = 0; i < Math.min(tickets.size(), lote.size()); i++) {
            if (tickets.get(i) instanceof Map<?, ?> ticket && "error".equals(ticket.get("status"))) {
                Object detalles = ticket.get("details");
                if (detalles instanceof Map<?, ?> d && "DeviceNotRegistered".equals(d.get("error"))) {
                    invalidos.add(lote.get(i).expoToken());
                } else {
                    log.warn("Expo rechazó una notificación: {}", ticket.get("message"));
                }
            }
        }
        return invalidos;
    }
}
