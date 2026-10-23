package pe.epsel.aguaya.auth.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.OffsetDateTime;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import pe.epsel.aguaya.auth.entity.CodigoOtp;
import pe.epsel.aguaya.auth.entity.PropositoOtp;
import pe.epsel.aguaya.auth.repository.CodigoOtpRepository;
import pe.epsel.aguaya.common.config.AguayaProperties;
import pe.epsel.aguaya.common.exception.NegocioException;

/**
 * Emisión y verificación de códigos OTP: 6 dígitos, guardados hasheados,
 * con vigencia, intentos máximos y límite de envíos por hora por teléfono.
 */
@Service
public class OtpService {

    private final CodigoOtpRepository codigoOtpRepository;
    private final OtpSender otpSender;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final AguayaProperties.Otp config;
    private final SecureRandom random = new SecureRandom();

    public OtpService(CodigoOtpRepository codigoOtpRepository, OtpSender otpSender, PasswordEncoder passwordEncoder,
                      Clock clock, AguayaProperties properties, Environment environment) {
        this.codigoOtpRepository = codigoOtpRepository;
        this.otpSender = otpSender;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.config = properties.otp();
        if (config.exponerCodigo() && environment.acceptsProfiles(Profiles.of("prod"))) {
            throw new IllegalStateException("aguaya.otp.exponer-codigo no puede estar activo en el perfil prod");
        }
    }

    /**
     * @param codigo el código en claro, solo si {@code aguaya.otp.exponer-codigo=true}; si no, {@code null}
     */
    public record OtpEmitido(OffsetDateTime expiraEn, String codigo) {
    }

    @Transactional
    public OtpEmitido emitir(String telefono, PropositoOtp proposito) {
        OffsetDateTime ahora = OffsetDateTime.now(clock);

        long enviadosUltimaHora = codigoOtpRepository.countByTelefonoAndCreadoEnAfter(telefono, ahora.minusHours(1));
        if (enviadosUltimaHora >= config.maxEnviosHora()) {
            throw new NegocioException(HttpStatus.TOO_MANY_REQUESTS, "OTP_LIMITE_ENVIOS",
                    "Se alcanzó el máximo de " + config.maxEnviosHora() + " códigos por hora. Intente más tarde");
        }

        codigoOtpRepository.invalidarVigentes(telefono, proposito);

        String codigo = String.format("%06d", random.nextInt(1_000_000));
        OffsetDateTime expiraEn = ahora.plusMinutes(config.vigenciaMinutos());
        codigoOtpRepository.save(new CodigoOtp(telefono, passwordEncoder.encode(codigo), proposito, ahora, expiraEn));

        otpSender.enviar(telefono, codigo);
        return new OtpEmitido(expiraEn, config.exponerCodigo() ? codigo : null);
    }

    /**
     * Verifica y consume el código. Corre en su propia transacción para que un intento
     * fallido quede registrado aunque la operación que lo invoca se revierta.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = NegocioException.class)
    public void verificar(String telefono, PropositoOtp proposito, String codigo) {
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        CodigoOtp otp = codigoOtpRepository
                .findFirstByTelefonoAndPropositoAndUsadoFalseOrderByCreadoEnDesc(telefono, proposito)
                .orElseThrow(() -> new NegocioException(HttpStatus.BAD_REQUEST, "OTP_NO_VIGENTE",
                        "No hay un código vigente para este teléfono. Solicite uno nuevo"));

        if (otp.expirado(ahora)) {
            otp.setUsado(true);
            throw new NegocioException(HttpStatus.BAD_REQUEST, "OTP_EXPIRADO",
                    "El código expiró. Solicite uno nuevo");
        }

        if (!passwordEncoder.matches(codigo, otp.getCodigoHash())) {
            otp.setIntentos(otp.getIntentos() + 1);
            if (otp.getIntentos() >= config.maxIntentos()) {
                otp.setUsado(true);
                throw new NegocioException(HttpStatus.BAD_REQUEST, "OTP_INTENTOS_AGOTADOS",
                        "Se agotaron los intentos. Solicite un nuevo código");
            }
            throw new NegocioException(HttpStatus.BAD_REQUEST, "OTP_INCORRECTO", "El código es incorrecto");
        }

        otp.setUsado(true);
    }
}
