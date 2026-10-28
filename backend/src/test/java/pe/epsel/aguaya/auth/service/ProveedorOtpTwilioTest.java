package pe.epsel.aguaya.auth.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import pe.epsel.aguaya.common.config.AguayaProperties;

class ProveedorOtpTwilioTest {

    private static final String TELEFONO = "+51987654321";
    private static final String URL = ProveedorOtpTwilio.URL_BASE + "VA123";

    private MockRestServiceServer servidor;
    private ProveedorOtpTwilio proveedor;

    private static AguayaProperties properties(String sid, String token, String verifySid) {
        return new AguayaProperties(ZoneId.of("America/Lima"),
                new AguayaProperties.Cors(List.of("http://localhost")),
                new AguayaProperties.Geocerca(50, 120),
                new AguayaProperties.Jwt("x".repeat(32), 15, 30),
                new AguayaProperties.Otp("twilio", false, 5, 3, 3),
                new AguayaProperties.Twilio(sid, token, verifySid));
    }

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        proveedor = new ProveedorOtpTwilio(builder, properties("AC1", "secreto", "VA123"));
    }

    @Test
    void enviarPideUnSmsATwilioConAutenticacionBasica() {
        String basic = "Basic " + Base64.getEncoder().encodeToString("AC1:secreto".getBytes());
        servidor.expect(requestTo(URL + "/Verifications"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", basic))
                .andExpect(content().formDataContains(Map.of("To", TELEFONO, "Channel", "sms")))
                .andRespond(withSuccess("{\"status\":\"pending\"}", MediaType.APPLICATION_JSON));

        proveedor.enviar(TELEFONO);

        servidor.verify();
    }

    @Test
    void enviarConDemasiadosIntentosDevuelve429() {
        servidor.expect(requestTo(URL + "/Verifications"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> proveedor.enviar(TELEFONO))
                .hasFieldOrPropertyWithValue("codigo", "OTP_LIMITE_ENVIOS");
    }

    @Test
    void enviarConFallaDeTwilioDevuelveErrorDeProveedor() {
        servidor.expect(requestTo(URL + "/Verifications"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> proveedor.enviar(TELEFONO))
                .hasFieldOrPropertyWithValue("codigo", "OTP_PROVEEDOR_ERROR");
    }

    @Test
    void verificarAprobado() {
        servidor.expect(requestTo(URL + "/VerificationCheck"))
                .andExpect(content().formDataContains(Map.of("To", TELEFONO, "Code", "123456")))
                .andRespond(withSuccess("{\"status\":\"approved\"}", MediaType.APPLICATION_JSON));

        assertThatCode(() -> proveedor.verificar(TELEFONO, "123456")).doesNotThrowAnyException();
    }

    @Test
    void verificarCodigoIncorrecto() {
        servidor.expect(requestTo(URL + "/VerificationCheck"))
                .andRespond(withSuccess("{\"status\":\"pending\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> proveedor.verificar(TELEFONO, "000000"))
                .hasFieldOrPropertyWithValue("codigo", "OTP_INCORRECTO");
    }

    @Test
    void verificarSinVerificacionPendiente() {
        servidor.expect(requestTo(URL + "/VerificationCheck"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> proveedor.verificar(TELEFONO, "123456"))
                .hasFieldOrPropertyWithValue("codigo", "OTP_NO_VIGENTE");
    }

    @Test
    void sinCredencialesNoArranca() {
        assertThatThrownBy(() -> new ProveedorOtpTwilio(RestClient.builder(), properties("", "", "")))
                .isInstanceOf(IllegalStateException.class);
    }
}
