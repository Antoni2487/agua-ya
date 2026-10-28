package pe.epsel.aguaya.incidencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.jayway.jsonpath.JsonPath;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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
 * Reasignación manual de paradas pendientes (RF07): el administrador elige otra ruta del mismo sector.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class ReasignacionIntegrationTest extends EscenarioRutaHoy {

    @MockitoBean
    private ProveedorPush proveedor;

    @Test
    void rutaInterrumpidaPasaSusPendientesALaRutaDeLaTarde() throws Exception {
        Escenario e = escenario();
        String admin = login("10000001", "Admin2026!");
        iniciar(e).andExpect(status().isOk());
        Parada atendida = e.paradas().get(0);
        Parada pendiente = e.paradas().get(1);
        enviarPosicion(e, atendida.latitud(), atendida.longitud()).andExpect(status().isNoContent());
        mockMvc.perform(conToken(e.tokenConductor(), patch("/api/conductor/paradas/" + atendida.id() + "/atender")))
                .andExpect(status().isOk());
        long incidencia = reportar(e.tokenConductor(), e.rutaId(), "AVERIA", true);

        // La ruta de la tarde ya tiene los dos puntos; se quita el pendiente para ver que se copia.
        long tarde = programar(admin, e.sectorId(), "TARDE");
        jdbc.update("DELETE FROM parada WHERE ruta_id = ? AND punto_entrega_id = "
                + "(SELECT punto_entrega_id FROM parada WHERE id = ?)", tarde, pendiente.id());
        String tokenPush = "ExponentPushToken[reasig-" + SECUENCIA.incrementAndGet() + "]";
        String vecino = vecinoDelSector(e);
        // Su vivienda queda junto al punto que se reasigna.
        jdbc.update("UPDATE vecino SET latitud = ?, longitud = ? WHERE sector_id = ?",
                pendiente.latitud(), pendiente.longitud(), e.sectorId());
        mockMvc.perform(conToken(vecino, post("/api/dispositivos")).content("{\"expoToken\":\"" + tokenPush + "\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(conToken(admin, get("/api/admin/incidencias/" + incidencia + "/sugerencias-reasignacion")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paradasPendientes").value(1))
                .andExpect(jsonPath("$.rutas[*].rutaId", Matchers.contains((int) tarde)));

        reasignar(admin, incidencia, tarde)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paradasReasignadas").value(1))
                .andExpect(jsonPath("$.paradasYaEnDestino").value(0))
                .andExpect(jsonPath("$.incidencia.estado").value("REASIGNADA"))
                .andExpect(jsonPath("$.incidencia.rutaDestinoId").value((int) tarde))
                .andExpect(jsonPath("$.incidencia.resueltaPorNombre").isNotEmpty());

        assertThat(estado(atendida.id())).isEqualTo("ATENDIDA");
        assertThat(estado(pendiente.id())).isEqualTo("REASIGNADA");
        mockMvc.perform(conToken(admin, get("/api/admin/rutas/" + tarde)))
                .andExpect(jsonPath("$.paradas.length()").value(2))
                .andExpect(jsonPath("$.paradas[*].orden", Matchers.contains(1, 2)))
                .andExpect(jsonPath("$.paradas[1].estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.paradas[1].latitud").value(pendiente.latitud()));

        assertThat(esperarPush(tokenPush)).singleElement()
                .satisfies(m -> assertThat(m.datos()).containsEntry("rutaId", tarde));

        reasignar(admin, incidencia, tarde)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("INCIDENCIA_NO_ABIERTA"));
    }

    @Test
    void noDuplicaLoQueLaRutaDestinoYaTieneYElOrigenPuedeCerrarse() throws Exception {
        Escenario e = escenario();
        String admin = login("10000001", "Admin2026!");
        iniciar(e).andExpect(status().isOk());
        long incidencia = reportar(e.tokenConductor(), e.rutaId(), "ACCESO_BLOQUEADO", false);
        long tarde = programar(admin, e.sectorId(), "TARDE");

        reasignar(admin, incidencia, tarde)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paradasReasignadas").value(0))
                .andExpect(jsonPath("$.paradasYaEnDestino").value(2));
        mockMvc.perform(conToken(admin, get("/api/admin/rutas/" + tarde)))
                .andExpect(jsonPath("$.paradas.length()").value(2));

        // Todas las paradas del origen quedaron en estado final: el conductor puede cerrar su ruta.
        mockMvc.perform(conToken(e.tokenConductor(), patch("/api/conductor/rutas/" + e.rutaId() + "/finalizar")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruta.estado").value("COMPLETADA"));
    }

    @Test
    void validaOrigenYDestino() throws Exception {
        String admin = login("10000001", "Admin2026!");

        Escenario programada = escenario();
        long deProgramada = reportar(vecinoDelSector(programada), programada.rutaId(), "NO_LLEGO", false);
        long tardeProgramada = programar(admin, programada.sectorId(), "TARDE");
        reasignar(admin, deProgramada, tardeProgramada)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("RUTA_ORIGEN_NO_REASIGNABLE"));

        Escenario e = escenario();
        iniciar(e).andExpect(status().isOk());
        long incidencia = reportar(e.tokenConductor(), e.rutaId(), "AVERIA", true);
        reasignar(admin, incidencia, e.rutaId())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("MISMA_RUTA"));
        reasignar(admin, incidencia, tardeProgramada)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("RUTA_DESTINO_OTRO_SECTOR"));
        reasignar(admin, incidencia, 999_999).andExpect(status().isNotFound());
        mockMvc.perform(conToken(admin, post("/api/admin/incidencias/" + incidencia + "/reasignar")).content("{}"))
                .andExpect(status().isBadRequest());

        long completada = programar(admin, e.sectorId(), "TARDE");
        jdbc.update("UPDATE ruta SET estado = 'COMPLETADA' WHERE id = ?", completada);
        reasignar(admin, incidencia, completada)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("RUTA_DESTINO_NO_DISPONIBLE"));

        mockMvc.perform(conToken(e.tokenConductor(), post("/api/admin/incidencias/" + incidencia + "/reasignar"))
                        .content("{\"rutaDestinoId\":1}"))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- helpers

    private long reportar(String token, long rutaId, String tipo, boolean detieneRuta) throws Exception {
        String body = mockMvc.perform(conToken(token, post("/api/incidencias")).content(
                        "{\"rutaId\":%d,\"tipo\":\"%s\",\"detieneRuta\":%s}".formatted(rutaId, tipo, detieneRuta)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long programar(String admin, long sectorId, String turno) throws Exception {
        long planta = jdbc.queryForObject("SELECT min(id) FROM punto_llenado WHERE tipo = 'PLANTA'", Long.class);
        String body = mockMvc.perform(conToken(admin, post("/api/admin/rutas")).content("""
                        {"sectorId":%d,"fecha":"%s","turno":"%s","puntoLlenadoId":%d}"""
                        .formatted(sectorId, HOY, turno, planta)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.ruta.id")).longValue();
    }

    private ResultActions reasignar(String admin, long incidenciaId, long destinoId) throws Exception {
        return mockMvc.perform(conToken(admin, post("/api/admin/incidencias/" + incidenciaId + "/reasignar"))
                .content("{\"rutaDestinoId\":%d}".formatted(destinoId)));
    }

    private String estado(long paradaId) {
        return jdbc.queryForObject("SELECT estado FROM parada WHERE id = ?", String.class, paradaId);
    }

    /** Los avisos push son asíncronos. */
    @SuppressWarnings("unchecked")
    private List<MensajePush> esperarPush(String token) throws InterruptedException {
        long limite = System.currentTimeMillis() + 5000;
        List<MensajePush> recibidos = List.of();
        while (recibidos.isEmpty() && System.currentTimeMillis() < limite) {
            Thread.sleep(50);
            recibidos = Mockito.mockingDetails(proveedor).getInvocations().stream()
                    .map(Invocation::getArguments)
                    .flatMap(a -> ((List<MensajePush>) a[0]).stream())
                    .filter(m -> m.expoToken().equals(token) && "PARADA_REASIGNADA".equals(m.datos().get("tipo")))
                    .toList();
        }
        return recibidos;
    }
}
