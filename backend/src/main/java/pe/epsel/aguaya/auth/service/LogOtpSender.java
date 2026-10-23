package pe.epsel.aguaya.auth.service;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Envío simulado: imprime el código en el log. Solo para desarrollo.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "aguaya.otp.provider", havingValue = "log", matchIfMissing = true)
public class LogOtpSender implements OtpSender {

    @Override
    public void enviar(String telefono, String codigo) {
        log.info("[OTP] Código para {}: {}", telefono, codigo);
    }
}
