package pe.epsel.aguaya.notificacion.service;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.ECPrivateKey;
import java.time.Clock;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import pe.epsel.aguaya.notificacion.config.WebPushProperties;

/**
 * Web Push estándar (sin SDK): cifra cada mensaje para su navegador ({@link CifradoWebPush}) y lo envía al
 * endpoint de la suscripción con autorización VAPID. Los servicios de push responden 201; 404 o 410 significan
 * que la suscripción ya no existe (el vecino desinstaló, borró datos o revocó el permiso).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "aguaya.web-push.provider", havingValue = "vapid")
public class ProveedorWebPushVapid implements ProveedorWebPush {

    /** El JWT VAPID puede durar hasta 24 h; se firma uno por servicio de push en cada envío. */
    private static final long VIGENCIA_JWT_SEGUNDOS = 12 * 3600;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final WebPushProperties properties;
    private final ECPrivateKey clavePrivada;

    public ProveedorWebPushVapid(RestClient.Builder builder, ObjectMapper objectMapper, Clock clock,
                                 WebPushProperties properties) {
        if (!StringUtils.hasText(properties.clavePublica()) || !StringUtils.hasText(properties.clavePrivada())) {
            throw new IllegalStateException("WEB_PUSH_PROVIDER=vapid requiere WEB_PUSH_CLAVE_PUBLICA y WEB_PUSH_CLAVE_PRIVADA");
        }
        CifradoWebPush.clavePublica(CifradoWebPush.decodificar(properties.clavePublica()));
        this.clavePrivada = CifradoWebPush.clavePrivada(properties.clavePrivada());
        this.restClient = builder.build();
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.properties = properties;
    }

    @Override
    public Set<String> enviar(List<MensajeWebPush> mensajes) {
        Set<String> vencidos = new HashSet<>();
        Map<String, String> jwtPorServicio = new HashMap<>();
        long expira = clock.instant().getEpochSecond() + VIGENCIA_JWT_SEGUNDOS;
        for (MensajeWebPush mensaje : mensajes) {
            try {
                String audiencia = origen(mensaje.endpoint());
                String jwt = jwtPorServicio.computeIfAbsent(audiencia,
                        a -> CifradoWebPush.jwtVapid(a, properties.sujeto(), expira, clavePrivada));
                byte[] cuerpo = CifradoWebPush.cifrar(carga(mensaje), mensaje.p256dh(), mensaje.auth());
                restClient.post()
                        .uri(URI.create(mensaje.endpoint()))
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header("Content-Encoding", "aes128gcm")
                        .header("TTL", String.valueOf(properties.ttlSegundos()))
                        .header("Urgency", "high")
                        .header("Authorization", "vapid t=" + jwt + ", k=" + properties.clavePublica())
                        .body(cuerpo)
                        .retrieve()
                        .toBodilessEntity();
            } catch (RestClientResponseException e) {
                int status = e.getStatusCode().value();
                if (status == 404 || status == 410) {
                    vencidos.add(mensaje.endpoint());
                } else {
                    log.warn("El servicio de push rechazó una notificación ({}): {}", status, e.getResponseBodyAsString());
                }
            } catch (RestClientException | IllegalStateException e) {
                log.error("No se pudo enviar una notificación Web Push", e);
            }
        }
        return vencidos;
    }

    /** Lo que recibe el service worker de la web: título, cuerpo y datos para abrir la pantalla. */
    private byte[] carga(MensajeWebPush mensaje) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("titulo", mensaje.titulo());
        json.put("cuerpo", mensaje.cuerpo());
        json.put("datos", mensaje.datos());
        try {
            return objectMapper.writeValueAsString(json).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el aviso", e);
        }
    }

    /** {@code https://fcm.googleapis.com/fcm/send/abc} → {@code https://fcm.googleapis.com}. */
    static String origen(String endpoint) {
        URI uri = URI.create(endpoint);
        return uri.getScheme() + "://" + uri.getHost() + (uri.getPort() > 0 ? ":" + uri.getPort() : "");
    }
}
