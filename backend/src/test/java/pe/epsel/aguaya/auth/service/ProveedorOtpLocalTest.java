package pe.epsel.aguaya.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import pe.epsel.aguaya.auth.entity.CodigoOtp;
import pe.epsel.aguaya.auth.repository.CodigoOtpRepository;
import pe.epsel.aguaya.common.config.AguayaProperties;
import pe.epsel.aguaya.common.exception.NegocioException;

@ExtendWith(MockitoExtension.class)
class ProveedorOtpLocalTest {

    private static final String TELEFONO = "+51987654321";
    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    @Mock
    private CodigoOtpRepository repository;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-23T15:00:00Z"), LIMA);
    private final OffsetDateTime ahora = OffsetDateTime.now(clock);

    private ProveedorOtpLocal proveedor;

    @BeforeEach
    void setUp() {
        proveedor = crear(false, new MockEnvironment());
    }

    private ProveedorOtpLocal crear(boolean exponerCodigo, MockEnvironment environment) {
        AguayaProperties properties = new AguayaProperties(LIMA,
                new AguayaProperties.Cors(List.of("http://localhost")),
                new AguayaProperties.Geocerca(50, 120),
                new AguayaProperties.Jwt("x".repeat(32), 15, 30),
                new AguayaProperties.Otp("log", exponerCodigo, 5, 3, 3),
                new AguayaProperties.Twilio(null, null, null));
        return new ProveedorOtpLocal(repository, encoder, clock, properties, environment);
    }

    @Test
    void enviarGuardaElCodigoHasheadoYVenceEn5Minutos() {
        ProveedorOtp.OtpEnviado enviado = crear(true, new MockEnvironment()).enviar(TELEFONO);

        ArgumentCaptor<CodigoOtp> guardado = ArgumentCaptor.forClass(CodigoOtp.class);
        verify(repository).save(guardado.capture());
        assertThat(enviado.codigo()).matches("\\d{6}");
        assertThat(guardado.getValue().getCodigoHash()).isNotEqualTo(enviado.codigo());
        assertThat(encoder.matches(enviado.codigo(), guardado.getValue().getCodigoHash())).isTrue();
        assertThat(enviado.expiraEn()).isEqualTo(ahora.plusMinutes(5));
    }

    @Test
    void enviarNoExponeElCodigoSiNoEstaHabilitado() {
        assertThat(proveedor.enviar(TELEFONO).codigo()).isNull();
    }

    @Test
    void enviarInvalidaLosCodigosAnteriores() {
        proveedor.enviar(TELEFONO);
        verify(repository).invalidarVigentes(TELEFONO);
    }

    @Test
    void enviarRechazaElCuartoEnvioEnUnaHora() {
        when(repository.countByTelefonoAndCreadoEnAfter(TELEFONO, ahora.minusHours(1))).thenReturn(3L);

        assertThatThrownBy(() -> proveedor.enviar(TELEFONO))
                .isInstanceOfSatisfying(NegocioException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                    assertThat(e.getCodigo()).isEqualTo("OTP_LIMITE_ENVIOS");
                });
        verify(repository, never()).save(any());
    }

    @Test
    void exponerCodigoNoSePermiteEnProd() {
        MockEnvironment prod = new MockEnvironment();
        prod.setActiveProfiles("prod");
        assertThatThrownBy(() -> crear(true, prod)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void verificarCodigoCorrectoLoConsume() {
        CodigoOtp otp = vigente("123456");

        proveedor.verificar(TELEFONO, "123456");

        assertThat(otp.isUsado()).isTrue();
    }

    @Test
    void verificarCodigoIncorrectoSumaIntento() {
        CodigoOtp otp = vigente("123456");

        assertThatThrownBy(() -> proveedor.verificar(TELEFONO, "000000"))
                .hasFieldOrPropertyWithValue("codigo", "OTP_INCORRECTO");
        assertThat(otp.getIntentos()).isEqualTo(1);
        assertThat(otp.isUsado()).isFalse();
    }

    @Test
    void tercerIntentoFallidoInvalidaElCodigo() {
        CodigoOtp otp = vigente("123456");
        otp.setIntentos(2);

        assertThatThrownBy(() -> proveedor.verificar(TELEFONO, "000000"))
                .hasFieldOrPropertyWithValue("codigo", "OTP_INTENTOS_AGOTADOS");
        assertThat(otp.isUsado()).isTrue();
    }

    @Test
    void verificarCodigoExpiradoLoRechaza() {
        CodigoOtp otp = new CodigoOtp(TELEFONO, encoder.encode("123456"), ahora.minusMinutes(6), ahora.minusMinutes(1));
        when(repository.findFirstByTelefonoAndUsadoFalseOrderByCreadoEnDesc(TELEFONO)).thenReturn(Optional.of(otp));

        assertThatThrownBy(() -> proveedor.verificar(TELEFONO, "123456"))
                .hasFieldOrPropertyWithValue("codigo", "OTP_EXPIRADO");
        assertThat(otp.isUsado()).isTrue();
    }

    @Test
    void verificarSinCodigoVigente() {
        assertThatThrownBy(() -> proveedor.verificar(TELEFONO, "123456"))
                .hasFieldOrPropertyWithValue("codigo", "OTP_NO_VIGENTE");
    }

    private CodigoOtp vigente(String codigo) {
        CodigoOtp otp = new CodigoOtp(TELEFONO, encoder.encode(codigo), ahora.minusMinutes(1), ahora.plusMinutes(4));
        when(repository.findFirstByTelefonoAndUsadoFalseOrderByCreadoEnDesc(TELEFONO)).thenReturn(Optional.of(otp));
        return otp;
    }
}
