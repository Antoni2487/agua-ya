package pe.epsel.aguaya.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import pe.epsel.aguaya.common.config.AguayaProperties;
import pe.epsel.aguaya.common.seguridad.UsuarioAutenticado;
import pe.epsel.aguaya.usuario.entity.Administrador;
import pe.epsel.aguaya.usuario.entity.Rol;

class JwtServiceTest {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final Instant AHORA = Instant.parse("2026-10-23T15:00:00Z");

    private static JwtService crear(String secret, Clock clock) {
        return new JwtService(new AguayaProperties(LIMA,
                new AguayaProperties.Cors(List.of("http://localhost")),
                new AguayaProperties.Geocerca(50),
                new AguayaProperties.Jwt(secret, 15, 30),
                new AguayaProperties.Otp("log", false, 5, 3, 3)), clock);
    }

    private static Administrador admin() {
        Administrador admin = new Administrador("12345678", "Ana", "Pérez", "+51900000001");
        ReflectionTestUtils.setField(admin, "id", 7L);
        return admin;
    }

    @Test
    void tokenValidoDevuelveIdYRol() {
        JwtService jwt = crear("a".repeat(32), Clock.fixed(AHORA, LIMA));

        String token = jwt.generarAccessToken(admin());

        assertThat(jwt.validar(token)).contains(new UsuarioAutenticado(7L, Rol.ADMIN));
        assertThat(jwt.vigenciaSegundos()).isEqualTo(15 * 60);
    }

    @Test
    void tokenExpiradoEsRechazado() {
        String token = crear("a".repeat(32), Clock.fixed(AHORA, LIMA)).generarAccessToken(admin());
        JwtService enElFuturo = crear("a".repeat(32), Clock.fixed(AHORA.plus(Duration.ofMinutes(16)), LIMA));

        assertThat(enElFuturo.validar(token)).isEmpty();
    }

    @Test
    void tokenFirmadoConOtraClaveEsRechazado() {
        String token = crear("b".repeat(32), Clock.fixed(AHORA, LIMA)).generarAccessToken(admin());

        assertThat(crear("a".repeat(32), Clock.fixed(AHORA, LIMA)).validar(token)).isEmpty();
    }

    @Test
    void tokenMalFormadoEsRechazado() {
        assertThat(crear("a".repeat(32), Clock.fixed(AHORA, LIMA)).validar("no-es-un-jwt")).isEmpty();
    }
}
