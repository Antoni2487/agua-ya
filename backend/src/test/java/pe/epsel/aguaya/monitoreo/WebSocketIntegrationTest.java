package pe.epsel.aguaya.monitoreo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import pe.epsel.aguaya.TestcontainersConfig;
import pe.epsel.aguaya.soporte.EscenarioRutaHoy;

/**
 * STOMP en {@code /ws}: autenticación en el CONNECT y autorización por tópico (regla 9).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class WebSocketIntegrationTest extends EscenarioRutaHoy {

    @LocalServerPort
    private int puerto;

    @Autowired
    private ObjectMapper objectMapper;

    private WebSocketStompClient cliente;

    @BeforeEach
    void cliente() {
        cliente = new WebSocketStompClient(new StandardWebSocketClient());
        MappingJackson2MessageConverter conversor = new MappingJackson2MessageConverter();
        conversor.setObjectMapper(objectMapper);
        cliente.setMessageConverter(conversor);
    }

    @AfterEach
    void cerrar() {
        cliente.stop();
    }

    @Test
    void adminRecibeLaPosicionYLosEventosEnTiempoReal() throws Exception {
        Escenario e = escenario();
        StompSession sesion = conectar(login("10000001", "Admin2026!"));
        BlockingQueue<Map<String, Object>> posiciones = suscribir(sesion, "/topic/rutas/" + e.rutaId() + "/posicion");
        BlockingQueue<Map<String, Object>> eventos = suscribir(sesion, "/topic/admin/rutas");
        Thread.sleep(300); // la suscripción se procesa de forma asíncrona

        iniciar(e).andExpect(status().isOk());
        Parada p = e.paradas().get(0);
        enviarPosicion(e, p.latitud(), p.longitud()).andExpect(status().isNoContent());

        Map<String, Object> evento = esperarEvento(eventos, "RUTA_INICIADA", e.rutaId());
        assertThat(evento).isNotNull();
        Map<String, Object> posicion = posiciones.poll(5, TimeUnit.SECONDS);
        assertThat(posicion).isNotNull();
        assertThat(((Number) posicion.get("rutaId")).longValue()).isEqualTo(e.rutaId());
        assertThat(((Number) posicion.get("latitud")).doubleValue()).isCloseTo(p.latitud(),
                org.assertj.core.data.Offset.offset(1e-6));
    }

    @Test
    void adminRecibeLaAlertaDeIncidencia() throws Exception {
        Escenario e = escenario();
        StompSession sesion = conectar(login("10000001", "Admin2026!"));
        BlockingQueue<Map<String, Object>> alertas = suscribir(sesion, "/topic/admin/incidencias");
        Thread.sleep(300);

        mockMvc.perform(conToken(e.tokenConductor(),
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/incidencias"))
                        .content("{\"rutaId\":%d,\"tipo\":\"AGUA_AGOTADA\"}".formatted(e.rutaId())))
                .andExpect(status().isCreated());

        long limite = System.currentTimeMillis() + 5000;
        Map<String, Object> alerta = null;
        while (alerta == null && System.currentTimeMillis() < limite) {
            Map<String, Object> m = alertas.poll(500, TimeUnit.MILLISECONDS);
            if (m != null && ((Number) m.get("rutaId")).longValue() == e.rutaId()) {
                alerta = m;
            }
        }
        assertThat(alerta).isNotNull();
        assertThat(alerta.get("tipo")).isEqualTo("AGUA_AGOTADA");
        assertThat(alerta.get("estado")).isEqualTo("ABIERTA");
    }

    @Test
    void sinTokenNoSePuedeConectar() {
        assertThatThrownBy(() -> cliente.connectAsync("ws://localhost:" + puerto + "/ws", new WebSocketHttpHeaders(),
                new StompHeaders(), new StompSessionHandlerAdapter() {
                }).get(5, TimeUnit.SECONDS))
                .isInstanceOf(Exception.class);
    }

    @Test
    void vecinoDeOtroSectorNoRecibeLaPosicion() throws Exception {
        Escenario e = escenario();
        iniciar(e).andExpect(status().isOk());

        StompSession sesion = conectar(login("30000004", "Vecino2026!"));
        BlockingQueue<Map<String, Object>> posiciones = suscribir(sesion, "/topic/rutas/" + e.rutaId() + "/posicion");
        Thread.sleep(300);

        Parada p = e.paradas().get(0);
        enviarPosicion(e, p.latitud(), p.longitud()).andExpect(status().isNoContent());

        assertThat(posiciones.poll(2, TimeUnit.SECONDS)).isNull();
        assertThat(sesion.isConnected()).isFalse();
    }

    @Test
    void conductorNoPuedeSuscribirseAlPanelDelAdmin() throws Exception {
        Escenario e = escenario();
        StompSession sesion = conectar(e.tokenConductor());
        suscribir(sesion, "/topic/admin/rutas");
        Thread.sleep(1000);
        assertThat(sesion.isConnected()).isFalse();
    }

    // ---------------------------------------------------------------- helpers

    private StompSession conectar(String token) throws Exception {
        StompHeaders connect = new StompHeaders();
        connect.add("Authorization", "Bearer " + token);
        return cliente.connectAsync("ws://localhost:" + puerto + "/ws", new WebSocketHttpHeaders(), connect,
                new StompSessionHandlerAdapter() {
                }).get(5, TimeUnit.SECONDS);
    }

    private static BlockingQueue<Map<String, Object>> suscribir(StompSession sesion, String destino) {
        BlockingQueue<Map<String, Object>> recibidos = new LinkedBlockingQueue<>();
        sesion.subscribe(destino, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void handleFrame(StompHeaders headers, Object payload) {
                recibidos.add((Map<String, Object>) payload);
            }
        });
        return recibidos;
    }

    /** El tópico de admin es compartido: descarta eventos de otras rutas. */
    private static Map<String, Object> esperarEvento(BlockingQueue<Map<String, Object>> eventos, String tipo, long rutaId)
            throws InterruptedException {
        long limite = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < limite) {
            Map<String, Object> evento = eventos.poll(500, TimeUnit.MILLISECONDS);
            if (evento != null && tipo.equals(evento.get("tipo"))
                    && ((Number) evento.get("rutaId")).longValue() == rutaId) {
                return evento;
            }
        }
        return null;
    }
}
