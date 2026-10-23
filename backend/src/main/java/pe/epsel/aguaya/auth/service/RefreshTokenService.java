package pe.epsel.aguaya.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import pe.epsel.aguaya.auth.entity.RefreshToken;
import pe.epsel.aguaya.auth.repository.RefreshTokenRepository;
import pe.epsel.aguaya.common.config.AguayaProperties;
import pe.epsel.aguaya.common.exception.NegocioException;
import pe.epsel.aguaya.usuario.entity.Usuario;

/**
 * Refresh tokens opacos con rotación: cada uso revoca el token y emite uno nuevo.
 * Si llega un token ya revocado se asume robo y se revocan todos los del usuario.
 */
@Slf4j
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;
    private final int vigenciaDias;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, Clock clock,
                               AguayaProperties properties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.clock = clock;
        this.vigenciaDias = properties.jwt().refreshDias();
    }

    @Transactional
    public String emitir(Usuario usuario) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        OffsetDateTime expiraEn = OffsetDateTime.now(clock).plusDays(vigenciaDias);
        refreshTokenRepository.save(new RefreshToken(usuario, hash(token), expiraEn));
        return token;
    }

    /**
     * Valida y revoca el token. Devuelve el id de su usuario para emitir el nuevo par.
     * La revocación por reuso se confirma aunque se lance la excepción.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = NegocioException.class)
    public Long consumir(String token) {
        RefreshToken refresh = refreshTokenRepository.findByTokenHash(hash(token))
                .orElseThrow(RefreshTokenService::invalido);

        Usuario usuario = refresh.getUsuario();
        if (refresh.isRevocado()) {
            log.warn("Reuso de refresh token revocado del usuario {}; se revocan todas sus sesiones", usuario.getId());
            refreshTokenRepository.revocarTodosDelUsuario(usuario.getId());
            throw invalido();
        }
        if (refresh.expirado(OffsetDateTime.now(clock)) || !usuario.isActivo()) {
            throw invalido();
        }

        refresh.setRevocado(true);
        return usuario.getId();
    }

    @Transactional
    public void revocar(String token) {
        refreshTokenRepository.findByTokenHash(hash(token)).ifPresent(r -> r.setRevocado(true));
    }

    private static NegocioException invalido() {
        return new NegocioException(HttpStatus.UNAUTHORIZED, "REFRESH_INVALIDO",
                "La sesión expiró. Inicie sesión nuevamente");
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
