package pe.epsel.aguaya.monitoreo.config;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import pe.epsel.aguaya.auth.service.JwtService;
import pe.epsel.aguaya.common.seguridad.UsuarioAutenticado;
import pe.epsel.aguaya.monitoreo.service.AutorizacionSuscripcion;

/**
 * Autentica el CONNECT con el access token (cabecera STOMP {@code Authorization: Bearer ...}) y
 * autoriza cada SUBSCRIBE. Los clientes no envían mensajes: el servidor solo publica.
 */
@Component
@RequiredArgsConstructor
public class StompAutenticacionInterceptor implements ChannelInterceptor {

    private static final String PREFIJO = "Bearer ";

    private final JwtService jwtService;
    private final AutorizacionSuscripcion autorizacion;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        switch (accessor.getCommand()) {
            case CONNECT -> accessor.setUser(autenticar(accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION)));
            case SUBSCRIBE -> {
                if (!autorizacion.puedeSuscribirse(usuario(accessor), accessor.getDestination())) {
                    throw new MessagingException("ACCESO_DENEGADO: no puede suscribirse a " + accessor.getDestination());
                }
            }
            case SEND -> throw new MessagingException("El servidor no acepta mensajes de los clientes");
            default -> {
            }
        }
        return message;
    }

    private UsernamePasswordAuthenticationToken autenticar(String header) {
        UsuarioAutenticado usuario = header != null && header.startsWith(PREFIJO)
                ? jwtService.validar(header.substring(PREFIJO.length())).orElse(null)
                : null;
        if (usuario == null) {
            throw new MessagingException("NO_AUTENTICADO: envíe Authorization: Bearer <accessToken> en el CONNECT");
        }
        return new UsernamePasswordAuthenticationToken(usuario, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.rol().name())));
    }

    private static UsuarioAutenticado usuario(StompHeaderAccessor accessor) {
        return accessor.getUser() instanceof UsernamePasswordAuthenticationToken auth
                && auth.getPrincipal() instanceof UsuarioAutenticado u ? u : null;
    }
}
