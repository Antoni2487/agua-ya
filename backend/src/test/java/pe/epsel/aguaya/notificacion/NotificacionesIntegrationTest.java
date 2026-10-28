package pe.epsel.aguaya.notificacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import pe.epsel.aguaya.notificacion.service.MensajePush;
import pe.epsel.aguaya.notificacion.service.ProveedorPush;
import pe.epsel.aguaya.soporte.EscenarioRutaHoy;

/**
 * Notificaciones push al vecino: registro del celular, ruta iniciada, cisterna cerca y entrega registrada.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class NotificacionesIntegrationTest extends EscenarioRutaHoy {

    @MockitoBean
    private ProveedorPush proveedor;

    @Test
    void vecinoRecibeLosAvisosDeSuParada() throws Exception {
        Escenario e = escenario();
        String vecino = vecinoDelSector(e);
        String token = nuevoToken();
        registrar(vecino, token).andExpect(status().isNoContent());
        String otroSector = nuevoToken();
        registrar(login("30000004", "Vecino2026!"), otroSector).andExpect(status().isNoContent());

        iniciar(e).andExpect(status().isOk());
        assertThat(esperar(token, "RUTA_INICIADA")).singleElement()
                .satisfies(m -> assertThat(m.titulo()).isEqualTo("La cisterna inició su recorrido"));

        // La vivienda está junto a la primera parada: dos posiciones cerca, un solo aviso.
        Parada p = e.paradas().get(0);
        enviarPosicion(e, p.latitud(), p.longitud()).andExpect(status().isNoContent());
        enviarPosicion(e, p.latitud(), p.longitud()).andExpect(status().isNoContent());
        assertThat(esperar(token, "CISTERNA_CERCA")).singleElement()
                .satisfies(m -> assertThat(m.datos()).containsEntry("paradaId", p.id()));

        mockMvc.perform(conToken(e.tokenConductor(), patch("/api/conductor/paradas/" + p.id() + "/atender")))
                .andExpect(status().isOk());
        assertThat(esperar(token, "PARADA_ATENDIDA")).hasSize(1);

        assertThat(mensajesA(otroSector)).isEmpty();
        assertThat(jdbc.queryForObject("SELECT aviso_cercania_en IS NOT NULL FROM parada WHERE id = ?",
                Boolean.class, p.id())).isTrue();
    }

    @Test
    void sinCelularRegistradoNoSeEnviaNada() throws Exception {
        Escenario e = escenario();
        vecinoDelSector(e);
        iniciar(e).andExpect(status().isOk());
        Thread.sleep(300);
        assertThat(mensajes().stream().filter(m -> m.datos().get("rutaId").equals(e.rutaId()))).isEmpty();
    }

    @Test
    void registrarEsIdempotenteYElTokenPasaAQuienLoRegistra() throws Exception {
        String token = nuevoToken();
        String vecino1 = login("30000001", "Vecino2026!");
        registrar(vecino1, token).andExpect(status().isNoContent());
        registrar(vecino1, token).andExpect(status().isNoContent());
        assertThat(duenoDe(token)).isEqualTo("30000001");

        registrar(login("30000002", "Vecino2026!"), token).andExpect(status().isNoContent());
        assertThat(duenoDe(token)).isEqualTo("30000002");

        // Solo el dueño actual puede darlo de baja.
        baja(vecino1, token).andExpect(status().isNoContent());
        assertThat(duenoDe(token)).isEqualTo("30000002");
        baja(login("30000002", "Vecino2026!"), token).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM dispositivo_push WHERE expo_token = ?",
                Integer.class, token)).isZero();
    }

    @Test
    void validaElTokenYElRol() throws Exception {
        registrar(login("30000001", "Vecino2026!"), "no-es-un-token").andExpect(status().isBadRequest());
        registrar(login("10000001", "Admin2026!"), nuevoToken()).andExpect(status().isForbidden());
        registrar(login("20000001", "Conductor2026!"), nuevoToken()).andExpect(status().isNoContent());
    }

    @Test
    void tokenQueExpoReportaComoNoRegistradoSeDaDeBaja() throws Exception {
        Escenario e = escenario();
        String token = nuevoToken();
        registrar(vecinoDelSector(e), token).andExpect(status().isNoContent());
        when(proveedor.enviar(anyList())).thenReturn(Set.of(token));

        iniciar(e).andExpect(status().isOk());
        esperar(token, "RUTA_INICIADA");

        long limite = System.currentTimeMillis() + 5000;
        int quedan;
        do {
            quedan = jdbc.queryForObject("SELECT count(*) FROM dispositivo_push WHERE expo_token = ?",
                    Integer.class, token);
        } while (quedan > 0 && System.currentTimeMillis() < limite);
        assertThat(quedan).isZero();
    }

    // ---------------------------------------------------------------- helpers

    private static String nuevoToken() {
        return "ExponentPushToken[test-" + SECUENCIA.incrementAndGet() + "]";
    }

    private ResultActions registrar(String accessToken, String expoToken) throws Exception {
        return mockMvc.perform(conToken(accessToken, post("/api/dispositivos"))
                .content("{\"expoToken\":\"%s\",\"plataforma\":\"ANDROID\"}".formatted(expoToken)));
    }

    private ResultActions baja(String accessToken, String expoToken) throws Exception {
        return mockMvc.perform(conToken(accessToken, delete("/api/dispositivos"))
                .content("{\"expoToken\":\"%s\"}".formatted(expoToken)));
    }

    private String duenoDe(String expoToken) {
        return jdbc.queryForObject("SELECT p.dni FROM dispositivo_push d JOIN persona p ON p.id = d.persona_id "
                + "WHERE d.expo_token = ?", String.class, expoToken);
    }

    /** Todo lo que se pasó al proveedor (los envíos son asíncronos). */
    @SuppressWarnings("unchecked")
    private List<MensajePush> mensajes() {
        return org.mockito.Mockito.mockingDetails(proveedor).getInvocations().stream()
                .map(Invocation::getArguments)
                .flatMap(args -> ((List<MensajePush>) args[0]).stream())
                .toList();
    }

    private List<MensajePush> mensajesA(String expoToken) {
        return mensajes().stream().filter(m -> m.expoToken().equals(expoToken)).toList();
    }

    /** Espera a que llegue al menos un mensaje del tipo y deja un margen para detectar duplicados. */
    private List<MensajePush> esperar(String expoToken, String tipo) throws InterruptedException {
        long limite = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < limite && filtrar(expoToken, tipo).isEmpty()) {
            Thread.sleep(50);
        }
        Thread.sleep(200);
        return filtrar(expoToken, tipo);
    }

    private List<MensajePush> filtrar(String expoToken, String tipo) {
        return mensajesA(expoToken).stream().filter(m -> tipo.equals(m.datos().get("tipo"))).toList();
    }
}
