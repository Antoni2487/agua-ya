package pe.epsel.aguaya.notificacion.service;

import java.net.URI;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import pe.epsel.aguaya.common.exception.NegocioException;
import pe.epsel.aguaya.notificacion.config.WebPushProperties;
import pe.epsel.aguaya.notificacion.dto.ClaveWebPushResponse;
import pe.epsel.aguaya.notificacion.dto.SuscripcionWebPushRequest;
import pe.epsel.aguaya.notificacion.entity.SuscripcionWebPush;
import pe.epsel.aguaya.notificacion.repository.SuscripcionWebPushRepository;
import pe.epsel.aguaya.persona.repository.PersonaRepository;

/**
 * Suscripciones Web Push de la web del vecino.
 * <p>
 * El backend hace un POST al endpoint que manda el navegador, así que solo se aceptan los servicios de push de
 * los navegadores (Chrome/Edge/Android, Firefox, Safari): evita que alguien use al servidor para llamar a
 * otras direcciones (SSRF).
 */
@Service
@RequiredArgsConstructor
public class WebPushService {

    /** Dominios de los servicios de push de los navegadores; se acepta el dominio exacto o un subdominio. */
    static final List<String> SERVICIOS_PERMITIDOS = List.of(
            "fcm.googleapis.com",          // Chrome, Edge y navegadores Chromium (incluye Android)
            "push.services.mozilla.com",   // Firefox
            "notify.windows.com",          // Edge heredado (WNS)
            "push.apple.com");             // Safari (iOS 16.4+ con la web agregada a inicio, macOS)

    private final SuscripcionWebPushRepository repository;
    private final PersonaRepository personaRepository;
    private final WebPushProperties properties;
    private final Clock clock;

    public ClaveWebPushResponse clavePublica() {
        return new ClaveWebPushResponse(StringUtils.hasText(properties.clavePublica()) ? properties.clavePublica() : null);
    }

    /** Idempotente: si el endpoint ya existe se actualiza y, si era de otra persona, pasa a quien lo registra. */
    @Transactional
    public void suscribir(Long personaId, SuscripcionWebPushRequest request) {
        validarEndpoint(request.endpoint());
        validarClaves(request.claves());
        SuscripcionWebPush suscripcion = repository.findByEndpoint(request.endpoint())
                .orElseGet(() -> new SuscripcionWebPush(request.endpoint()));
        suscripcion.asignar(personaRepository.getReferenceById(personaId), request.claves().p256dh(),
                request.claves().auth(), OffsetDateTime.now(clock));
        repository.save(suscripcion);
    }

    /** Solo da de baja suscripciones propias y no falla si ya no existe. */
    @Transactional
    public void darDeBaja(Long personaId, String endpoint) {
        repository.eliminarDePersona(endpoint, personaId);
    }

    static void validarEndpoint(String endpoint) {
        URI uri;
        try {
            uri = URI.create(endpoint);
        } catch (IllegalArgumentException e) {
            throw invalida("El endpoint no es una URL válida");
        }
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
        boolean permitido = SERVICIOS_PERMITIDOS.stream().anyMatch(s -> host.equals(s) || host.endsWith("." + s));
        if (!"https".equals(uri.getScheme()) || !permitido) {
            throw invalida("El endpoint no es de un servicio de push de navegador conocido");
        }
    }

    private static void validarClaves(SuscripcionWebPushRequest.Claves claves) {
        boolean validas;
        try {
            CifradoWebPush.clavePublica(CifradoWebPush.decodificar(claves.p256dh()));
            validas = CifradoWebPush.decodificar(claves.auth()).length == 16;
        } catch (IllegalArgumentException e) {
            validas = false;
        }
        if (!validas) {
            throw invalida("Las claves de la suscripción no son válidas");
        }
    }

    private static NegocioException invalida(String mensaje) {
        return new NegocioException(HttpStatus.BAD_REQUEST, "SUSCRIPCION_INVALIDA", mensaje);
    }
}
