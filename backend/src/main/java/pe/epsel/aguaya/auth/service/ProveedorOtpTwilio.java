package pe.epsel.aguaya.auth.service;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import pe.epsel.aguaya.common.config.AguayaProperties;
import pe.epsel.aguaya.common.exception.NegocioException;

/**
 * Proveedor OTP de producción con Twilio Verify (API REST, sin SDK).
 * Twilio genera el código, envía el SMS y lo valida; también aplica sus propios
 * límites de vigencia, intentos y reenvíos (configurables en la consola de Twilio).
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "aguaya.otp.provider", havingValue = "twilio")
public class ProveedorOtpTwilio implements ProveedorOtp {

    static final String URL_BASE = "https://verify.twilio.com/v2/Services/";

    private final RestClient restClient;
    private final String verifySid;

    public ProveedorOtpTwilio(RestClient.Builder builder, AguayaProperties properties) {
        AguayaProperties.Twilio twilio = properties.twilio();
        if (!StringUtils.hasText(twilio.accountSid()) || !StringUtils.hasText(twilio.authToken())
                || !StringUtils.hasText(twilio.verifySid())) {
            throw new IllegalStateException(
                    "OTP_PROVIDER=twilio requiere TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN y TWILIO_VERIFY_SID");
        }
        this.verifySid = twilio.verifySid();
        this.restClient = builder
                .defaultHeaders(h -> h.setBasicAuth(twilio.accountSid(), twilio.authToken()))
                .build();
    }

    @Override
    public OtpEnviado enviar(String telefono) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", telefono);
        form.add("Channel", "sms");
        try {
            post("/Verifications", form);
            return new OtpEnviado(null, null);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 429) {
                throw new NegocioException(HttpStatus.TOO_MANY_REQUESTS, "OTP_LIMITE_ENVIOS",
                        "Se enviaron demasiados códigos a este teléfono. Intente más tarde");
            }
            if (e.getStatusCode().value() == 400) {
                throw new NegocioException(HttpStatus.BAD_REQUEST, "TELEFONO_INVALIDO",
                        "No se pudo enviar el SMS a ese número");
            }
            throw errorProveedor(e);
        } catch (RestClientException e) {
            throw errorProveedor(e);
        }
    }

    @Override
    public void verificar(String telefono, String codigo) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", telefono);
        form.add("Code", codigo);
        Map<?, ?> respuesta;
        try {
            respuesta = post("/VerificationCheck", form);
        } catch (RestClientResponseException e) {
            // 404: no hay verificación pendiente (expiró, ya se usó o se agotaron los intentos)
            if (e.getStatusCode().value() == 404) {
                throw new NegocioException(HttpStatus.BAD_REQUEST, "OTP_NO_VIGENTE",
                        "No hay un código vigente para este teléfono. Solicite uno nuevo");
            }
            if (e.getStatusCode().value() == 429) {
                throw new NegocioException(HttpStatus.BAD_REQUEST, "OTP_INTENTOS_AGOTADOS",
                        "Se agotaron los intentos. Solicite un nuevo código");
            }
            throw errorProveedor(e);
        } catch (RestClientException e) {
            throw errorProveedor(e);
        }

        if (respuesta == null || !"approved".equals(respuesta.get("status"))) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "OTP_INCORRECTO", "El código es incorrecto");
        }
    }

    private Map<?, ?> post(String ruta, MultiValueMap<String, String> form) {
        return restClient.post()
                .uri(URL_BASE + verifySid + ruta)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);
    }

    private static NegocioException errorProveedor(Exception e) {
        log.error("Error al llamar a Twilio Verify", e);
        return new NegocioException(HttpStatus.BAD_GATEWAY, "OTP_PROVEEDOR_ERROR",
                "No se pudo procesar el código de verificación. Intente nuevamente");
    }
}
