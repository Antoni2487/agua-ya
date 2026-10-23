package pe.epsel.aguaya.auth.service;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.stereotype.Service;

import pe.epsel.aguaya.common.config.AguayaProperties;
import pe.epsel.aguaya.common.seguridad.UsuarioAutenticado;
import pe.epsel.aguaya.usuario.entity.Rol;
import pe.epsel.aguaya.usuario.entity.Usuario;

/**
 * Access tokens JWT (HS256) de corta duración. {@code sub} = id del usuario, claim {@code rol}.
 */
@Service
public class JwtService {

    private static final String CLAIM_ROL = "rol";

    private final SecretKey clave;
    private final Duration vigencia;
    private final Clock clock;

    public JwtService(AguayaProperties properties, Clock clock) {
        this.clave = Keys.hmacShaKeyFor(properties.jwt().secret().getBytes(StandardCharsets.UTF_8));
        this.vigencia = Duration.ofMinutes(properties.jwt().accessMinutos());
        this.clock = clock;
    }

    public String generarAccessToken(Usuario usuario) {
        Instant ahora = clock.instant();
        return Jwts.builder()
                .subject(usuario.getId().toString())
                .claim(CLAIM_ROL, usuario.getRol().name())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(vigencia)))
                .signWith(clave)
                .compact();
    }

    public long vigenciaSegundos() {
        return vigencia.toSeconds();
    }

    /**
     * @return el usuario del token, o vacío si la firma es inválida, expiró o está mal formado
     */
    public Optional<UsuarioAutenticado> validar(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(clave)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(new UsuarioAutenticado(
                    Long.valueOf(claims.getSubject()),
                    Rol.valueOf(claims.get(CLAIM_ROL, String.class))));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
