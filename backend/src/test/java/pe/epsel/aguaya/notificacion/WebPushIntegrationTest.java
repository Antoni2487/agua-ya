package pe.epsel.aguaya.notificacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.invocation.Invocation;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;

import pe.epsel.aguaya.TestcontainersConfig;
import pe.epsel.aguaya.notificacion.service.CifradoWebPush;
import pe.epsel.aguaya.notificacion.service.MensajeWebPush;
import pe.epsel.aguaya.notificacion.service.ProveedorWebPush;
import pe.epsel.aguaya.soporte.EscenarioRutaHoy;

/**
 * Web Push a la web del vecino: suscripción del navegador y avisos de su parada.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class WebPushIntegrationTest extends EscenarioRutaHoy {

    private static final String AUTH = CifradoWebPush.codificar(new byte[16]);

    @MockitoBean
    private ProveedorWebPush proveedor;

    @Test
    void vecinoSuscritoRecibeElAvisoDeRutaIniciada() throws Exception {
        Escenario e = escenario();
        String endpoint = nuevoEndpoint();
        suscribir(vecinoDelSector(e), endpoint, clavePublicaNavegador()).andExpect(status().isNoContent());

        iniciar(e).andExpect(status().isOk());

        assertThat(esperar(endpoint, "RUTA_INICIADA")).singleElement()
                .satisfies(m -> {
                    assertThat(m.titulo()).isEqualTo("La cisterna inició su recorrido");
                    assertThat(m.auth()).isEqualTo(AUTH);
                });
    }

    @Test
    void suscripcionVencidaSeDaDeBaja() throws Exception {
        Escenario e = escenario();
        String endpoint = nuevoEndpoint();
        suscribir(vecinoDelSector(e), endpoint, clavePublicaNavegador()).andExpect(status().isNoContent());
        when(proveedor.enviar(anyList())).thenReturn(Set.of(endpoint));

        iniciar(e).andExpect(status().isOk());
        esperar(endpoint, "RUTA_INICIADA");

        long limite = System.currentTimeMillis() + 5000;
        int quedan;
        do {
            quedan = contar(endpoint);
        } while (quedan > 0 && System.currentTimeMillis() < limite);
        assertThat(quedan).isZero();
    }

    @Test
    void suscribirEsIdempotenteYPasaAQuienLaRegistra() throws Exception {
        String endpoint = nuevoEndpoint();
        String clave = clavePublicaNavegador();
        String vecino1 = login("30000001", "1234");
        suscribir(vecino1, endpoint, clave).andExpect(status().isNoContent());
        suscribir(vecino1, endpoint, clave).andExpect(status().isNoContent());
        assertThat(contar(endpoint)).isEqualTo(1);

        String vecino2 = login("30000002", "1234");
        suscribir(vecino2, endpoint, clave).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT p.dni FROM suscripcion_web_push s JOIN persona p ON p.id = s.persona_id "
                + "WHERE s.endpoint = ?", String.class, endpoint)).isEqualTo("30000002");

        // Solo el dueño actual puede darla de baja
        baja(vecino1, endpoint).andExpect(status().isNoContent());
        assertThat(contar(endpoint)).isEqualTo(1);
        baja(vecino2, endpoint).andExpect(status().isNoContent());
        assertThat(contar(endpoint)).isZero();
    }

    @Test
    void soloAceptaServiciosDePushConocidosYClavesValidas() throws Exception {
        String vecino = login("30000001", "1234");
        // Evita que el servidor haga POST a direcciones arbitrarias (SSRF)
        suscribir(vecino, "https://localhost:8080/api/admin/rutas", clavePublicaNavegador())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SUSCRIPCION_INVALIDA"));
        suscribir(vecino, "http://fcm.googleapis.com/fcm/send/x", clavePublicaNavegador())
                .andExpect(status().isBadRequest());
        suscribir(vecino, "https://fcm.googleapis.com.atacante.com/x", clavePublicaNavegador())
                .andExpect(status().isBadRequest());
        suscribir(vecino, nuevoEndpoint(), "no-es-una-clave")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SUSCRIPCION_INVALIDA"));
        // Firefox y Safari
        suscribir(vecino, "https://updates.push.services.mozilla.com/wpush/v2/" + SECUENCIA.incrementAndGet(),
                clavePublicaNavegador()).andExpect(status().isNoContent());
        suscribir(vecino, "https://web.push.apple.com/" + SECUENCIA.incrementAndGet(), clavePublicaNavegador())
                .andExpect(status().isNoContent());
    }

    @Test
    void soloElVecinoSeSuscribeYLaClavePublicaEsPublica() throws Exception {
        suscribir(login("10000001", "Admin2026!"), nuevoEndpoint(), clavePublicaNavegador())
                .andExpect(status().isForbidden());
        suscribir(login("20000001", "Conductor2026!"), nuevoEndpoint(), clavePublicaNavegador())
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/public/web-push/clave-publica")).andExpect(status().isOk());
    }

    // ---------------------------------------------------------------- helpers

    private static String nuevoEndpoint() {
        return "https://fcm.googleapis.com/fcm/send/test-" + SECUENCIA.incrementAndGet();
    }

    /** Clave pública P-256 como la genera un navegador. */
    private static String clavePublicaNavegador() throws Exception {
        KeyPairGenerator generador = KeyPairGenerator.getInstance("EC");
        generador.initialize(new ECGenParameterSpec("secp256r1"));
        return CifradoWebPush.codificar(
                CifradoWebPush.puntoSinComprimir((ECPublicKey) generador.generateKeyPair().getPublic()));
    }

    private ResultActions suscribir(String accessToken, String endpoint, String p256dh) throws Exception {
        return mockMvc.perform(conToken(accessToken, post("/api/vecino/web-push")).content("""
                {"endpoint":"%s","expirationTime":null,"keys":{"p256dh":"%s","auth":"%s"}}"""
                .formatted(endpoint, p256dh, AUTH)));
    }

    private ResultActions baja(String accessToken, String endpoint) throws Exception {
        return mockMvc.perform(conToken(accessToken, delete("/api/vecino/web-push"))
                .content("{\"endpoint\":\"%s\"}".formatted(endpoint)));
    }

    private int contar(String endpoint) {
        return jdbc.queryForObject("SELECT count(*) FROM suscripcion_web_push WHERE endpoint = ?", Integer.class,
                endpoint);
    }

    /** Todo lo que se pasó al proveedor (los envíos son asíncronos). */
    @SuppressWarnings("unchecked")
    private List<MensajeWebPush> mensajes() {
        return org.mockito.Mockito.mockingDetails(proveedor).getInvocations().stream()
                .map(Invocation::getArguments)
                .flatMap(args -> ((List<MensajeWebPush>) args[0]).stream())
                .toList();
    }

    /** Espera a que llegue al menos un mensaje del tipo y deja un margen para detectar duplicados. */
    private List<MensajeWebPush> esperar(String endpoint, String tipo) throws InterruptedException {
        long limite = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < limite && filtrar(endpoint, tipo).isEmpty()) {
            Thread.sleep(50);
        }
        Thread.sleep(200);
        return filtrar(endpoint, tipo);
    }

    private List<MensajeWebPush> filtrar(String endpoint, String tipo) {
        return mensajes().stream()
                .filter(m -> m.endpoint().equals(endpoint) && tipo.equals(m.datos().get("tipo")))
                .toList();
    }
}
