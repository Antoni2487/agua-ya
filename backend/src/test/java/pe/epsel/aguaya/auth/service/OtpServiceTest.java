package pe.epsel.aguaya.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import pe.epsel.aguaya.auth.entity.PropositoOtp;
import pe.epsel.aguaya.auth.repository.CodigoOtpRepository;
import pe.epsel.aguaya.common.config.AguayaProperties;
import pe.epsel.aguaya.common.exception.NegocioException;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    private static final String TELEFONO = "+51987654321";
    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    @Mock
    private CodigoOtpRepository repository;

    @Mock
    private OtpSender sender;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-23T15:00:00Z"), LIMA);
    private final OffsetDateTime ahora = OffsetDateTime.now(clock);

    private OtpService service;

    @BeforeEach
    void setUp() {
        service = crear(false, new MockEnvironment());
    }

    private OtpService crear(boolean exponerCodigo, MockEnvironment environment) {
        AguayaProperties properties = new AguayaProperties(LIMA,
                new AguayaProperties.Cors(List.of("http://localhost")),
                new AguayaProperties.Geocerca(50),
                new AguayaProperties.Jwt("x".repeat(32), 15, 30),
                new AguayaProperties.Otp("log", exponerCodigo, 5, 3, 3));
        return new OtpService(repository, sender, encoder, clock, properties, environment);
    }

    @Test
    void emitirGuardaHashEnviaCodigoDe6DigitosYVence5Minutos() {
        OtpService.OtpEmitido emitido = service.emitir(TELEFONO, PropositoOtp.REGISTRO);

        ArgumentCaptor<String> codigoEnviado = ArgumentCaptor.forClass(String.class);
        verify(sender).enviar(eq(TELEFONO), codigoEnviado.capture());
        assertThat(codigoEnviado.getValue()).matches("\\d{6}");

        ArgumentCaptor<CodigoOtp> guardado = ArgumentCaptor.forClass(CodigoOtp.class);
        verify(repository).save(guardado.capture());
        assertThat(guardado.getValue().getCodigoHash()).isNotEqualTo(codigoEnviado.getValue());
        assertThat(encoder.matches(codigoEnviado.getValue(), guardado.getValue().getCodigoHash())).isTrue();
        assertThat(emitido.expiraEn()).isEqualTo(ahora.plusMinutes(5));
        assertThat(emitido.codigo()).isNull();
    }

    @Test
    void emitirInvalidaLosCodigosAnteriores() {
        service.emitir(TELEFONO, PropositoOtp.LOGIN);
        verify(repository).invalidarVigentes(TELEFONO, PropositoOtp.LOGIN);
    }

    @Test
    void emitirDevuelveElCodigoSoloSiEstaHabilitado() {
        OtpService.OtpEmitido emitido = crear(true, new MockEnvironment()).emitir(TELEFONO, PropositoOtp.REGISTRO);
        assertThat(emitido.codigo()).matches("\\d{6}");
    }

    @Test
    void emitirRechazaElCuartoEnvioEnUnaHora() {
        when(repository.countByTelefonoAndCreadoEnAfter(TELEFONO, ahora.minusHours(1))).thenReturn(3L);

        assertThatThrownBy(() -> service.emitir(TELEFONO, PropositoOtp.REGISTRO))
                .isInstanceOfSatisfying(NegocioException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                    assertThat(e.getCodigo()).isEqualTo("OTP_LIMITE_ENVIOS");
                });
        verify(sender, never()).enviar(anyString(), anyString());
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

        service.verificar(TELEFONO, PropositoOtp.REGISTRO, "123456");

        assertThat(otp.isUsado()).isTrue();
    }

    @Test
    void verificarCodigoIncorrectoSumaIntento() {
        CodigoOtp otp = vigente("123456");

        assertThatThrownBy(() -> service.verificar(TELEFONO, PropositoOtp.REGISTRO, "000000"))
                .hasFieldOrPropertyWithValue("codigo", "OTP_INCORRECTO");
        assertThat(otp.getIntentos()).isEqualTo(1);
        assertThat(otp.isUsado()).isFalse();
    }

    @Test
    void tercerIntentoFallidoInvalidaElCodigo() {
        CodigoOtp otp = vigente("123456");
        otp.setIntentos(2);

        assertThatThrownBy(() -> service.verificar(TELEFONO, PropositoOtp.REGISTRO, "000000"))
                .hasFieldOrPropertyWithValue("codigo", "OTP_INTENTOS_AGOTADOS");
        assertThat(otp.isUsado()).isTrue();
    }

    @Test
    void verificarCodigoExpiradoLoRechaza() {
        CodigoOtp otp = new CodigoOtp(TELEFONO, encoder.encode("123456"), PropositoOtp.REGISTRO,
                ahora.minusMinutes(6), ahora.minusMinutes(1));
        when(repository.findFirstByTelefonoAndPropositoAndUsadoFalseOrderByCreadoEnDesc(TELEFONO,
                PropositoOtp.REGISTRO)).thenReturn(Optional.of(otp));

        assertThatThrownBy(() -> service.verificar(TELEFONO, PropositoOtp.REGISTRO, "123456"))
                .hasFieldOrPropertyWithValue("codigo", "OTP_EXPIRADO");
        assertThat(otp.isUsado()).isTrue();
    }

    @Test
    void verificarSinCodigoVigente() {
        assertThatThrownBy(() -> service.verificar(TELEFONO, PropositoOtp.LOGIN, "123456"))
                .hasFieldOrPropertyWithValue("codigo", "OTP_NO_VIGENTE");
    }

    private CodigoOtp vigente(String codigo) {
        CodigoOtp otp = new CodigoOtp(TELEFONO, encoder.encode(codigo), PropositoOtp.REGISTRO,
                ahora.minusMinutes(1), ahora.plusMinutes(4));
        when(repository.findFirstByTelefonoAndPropositoAndUsadoFalseOrderByCreadoEnDesc(TELEFONO,
                PropositoOtp.REGISTRO)).thenReturn(Optional.of(otp));
        return otp;
    }
}
