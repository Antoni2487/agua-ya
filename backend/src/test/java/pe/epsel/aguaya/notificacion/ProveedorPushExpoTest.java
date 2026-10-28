package pe.epsel.aguaya.notificacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import pe.epsel.aguaya.common.config.AguayaProperties;
import pe.epsel.aguaya.notificacion.service.MensajePush;
import pe.epsel.aguaya.notificacion.service.ProveedorPushExpo;

class ProveedorPushExpoTest {

    private static final String URL = "https://exp.host/--/api/v2/push/send";

    private MockRestServiceServer servidor;

    private ProveedorPushExpo crear(String accessToken) {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        return new ProveedorPushExpo(builder, new AguayaProperties(ZoneId.of("America/Lima"),
                new AguayaProperties.Cors(List.of("http://localhost")),
                new AguayaProperties.Geocerca(50, 120),
                new AguayaProperties.Jwt("x".repeat(32), 15, 30),
                new AguayaProperties.Otp("log", false, 5, 3, 3),
                new AguayaProperties.Twilio(null, null, null),
                new AguayaProperties.Push("expo", accessToken, 500)));
    }

    private static MensajePush mensaje(String token) {
        return new MensajePush(token, "Título", "Cuerpo", Map.of("tipo", "RUTA_INICIADA"));
    }

    @Test
    void enviaElLoteYDevuelveLosTokensNoRegistrados() {
        ProveedorPushExpo proveedor = crear("secreto");
        servidor.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer secreto"))
                .andExpect(jsonPath("$[0].to").value("ExponentPushToken[a]"))
                .andExpect(jsonPath("$[0].title").value("Título"))
                .andExpect(jsonPath("$[0].data.tipo").value("RUTA_INICIADA"))
                .andRespond(withSuccess("""
                        {"data":[{"status":"ok","id":"1"},
                                 {"status":"error","message":"no registrado","details":{"error":"DeviceNotRegistered"}}]}""",
                        MediaType.APPLICATION_JSON));

        assertThat(proveedor.enviar(List.of(mensaje("ExponentPushToken[a]"), mensaje("ExponentPushToken[b]"))))
                .containsExactly("ExponentPushToken[b]");
        servidor.verify();
    }

    @Test
    void parteEnLotesDe100() {
        ProveedorPushExpo proveedor = crear(null);
        servidor.expect(ExpectedCount.times(2), requestTo(URL))
                .andRespond(withSuccess("{\"data\":[]}", MediaType.APPLICATION_JSON));

        proveedor.enviar(IntStream.range(0, 150).mapToObj(i -> mensaje("ExponentPushToken[" + i + "]")).toList());
        servidor.verify();
    }

    @Test
    void unaFallaDeExpoNoSePropaga() {
        ProveedorPushExpo proveedor = crear(null);
        servidor.expect(requestTo(URL)).andRespond(withServerError());

        assertThat(proveedor.enviar(List.of(mensaje("ExponentPushToken[a]")))).isEmpty();
    }
}
