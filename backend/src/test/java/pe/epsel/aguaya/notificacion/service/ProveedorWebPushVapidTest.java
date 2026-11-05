package pe.epsel.aguaya.notificacion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import pe.epsel.aguaya.notificacion.config.WebPushProperties;

class ProveedorWebPushVapidTest {

    private static final String ENDPOINT_OK = "https://fcm.googleapis.com/fcm/send/vigente";
    private static final String ENDPOINT_VENCIDO = "https://fcm.googleapis.com/fcm/send/vencido";

    private MockRestServiceServer servidor;
    private ProveedorWebPushVapid proveedor;
    private WebPushProperties propiedades;
    private String p256dhNavegador;

    @BeforeEach
    void setUp() throws Exception {
        KeyPair vapid = parP256();
        propiedades = new WebPushProperties("vapid",
                CifradoWebPush.codificar(CifradoWebPush.puntoSinComprimir((ECPublicKey) vapid.getPublic())),
                CifradoWebPush.codificar(d32((ECPrivateKey) vapid.getPrivate())),
                "mailto:aguaya@example.com", 3600);
        p256dhNavegador = CifradoWebPush.codificar(CifradoWebPush.puntoSinComprimir((ECPublicKey) parP256().getPublic()));

        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        proveedor = new ProveedorWebPushVapid(builder, new ObjectMapper(),
                Clock.fixed(Instant.parse("2026-11-04T15:00:00Z"), ZoneOffset.UTC), propiedades);
    }

    @Test
    void enviaCifradoConVapidYDevuelveLasSuscripcionesVencidas() {
        servidor.expect(requestTo(ENDPOINT_OK))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Encoding", "aes128gcm"))
                .andExpect(header("TTL", "3600"))
                .andExpect(header("Authorization", Matchers.allOf(
                        Matchers.startsWith("vapid t="), Matchers.endsWith(", k=" + propiedades.clavePublica()))))
                .andRespond(withStatus(HttpStatus.CREATED));
        servidor.expect(requestTo(ENDPOINT_VENCIDO)).andRespond(withStatus(HttpStatus.GONE));

        Set<String> vencidos = proveedor.enviar(List.of(mensaje(ENDPOINT_OK), mensaje(ENDPOINT_VENCIDO)));

        assertThat(vencidos).containsExactly(ENDPOINT_VENCIDO);
        servidor.verify();
    }

    @Test
    void unErrorDelServicioNoDetieneLosDemasEnvios() {
        servidor.expect(requestTo(ENDPOINT_VENCIDO)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        servidor.expect(requestTo(ENDPOINT_OK)).andRespond(withStatus(HttpStatus.CREATED));

        assertThat(proveedor.enviar(List.of(mensaje(ENDPOINT_VENCIDO), mensaje(ENDPOINT_OK)))).isEmpty();
        servidor.verify();
    }

    @Test
    void sinClavesNoArranca() {
        WebPushProperties sinClaves = new WebPushProperties("vapid", "", "", "mailto:x@example.com", 3600);
        assertThatThrownBy(() -> new ProveedorWebPushVapid(RestClient.builder(), new ObjectMapper(), Clock.systemUTC(),
                sinClaves)).isInstanceOf(IllegalStateException.class).hasMessageContaining("WEB_PUSH_CLAVE_PRIVADA");
    }

    @Test
    void laAudienciaEsElOrigenDelEndpoint() {
        assertThat(ProveedorWebPushVapid.origen("https://fcm.googleapis.com/fcm/send/abc"))
                .isEqualTo("https://fcm.googleapis.com");
        assertThat(ProveedorWebPushVapid.origen("https://web.push.apple.com:443/x"))
                .isEqualTo("https://web.push.apple.com:443");
    }

    private MensajeWebPush mensaje(String endpoint) {
        return new MensajeWebPush(endpoint, p256dhNavegador, CifradoWebPush.codificar(new byte[16]),
                "La cisterna está cerca", "Ten listos tus recipientes", Map.of("tipo", "CISTERNA_CERCA"));
    }

    private static KeyPair parP256() throws Exception {
        KeyPairGenerator generador = KeyPairGenerator.getInstance("EC");
        generador.initialize(new ECGenParameterSpec("secp256r1"));
        return generador.generateKeyPair();
    }

    private static byte[] d32(ECPrivateKey clave) {
        byte[] d = clave.getS().toByteArray();
        byte[] resultado = new byte[32];
        int largo = Math.min(d.length, 32);
        System.arraycopy(d, d.length - largo, resultado, 32 - largo, largo);
        return resultado;
    }
}
