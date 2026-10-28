package pe.epsel.aguaya.ruta;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.ResultActions;

import pe.epsel.aguaya.TestcontainersConfig;
import pe.epsel.aguaya.soporte.EscenarioRutaHoy;

/**
 * Ciclo de viajes de la ruta en curso: salida cargada, reparto y cierre.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class ViajesIntegrationTest extends EscenarioRutaHoy {

    @Test
    void cicloCompletoAsociaLasParadasAlViajeYCierraLaRuta() throws Exception {
        Escenario e = escenario();
        iniciar(e).andExpect(status().isOk());

        long viaje1 = idViaje(iniciarViaje(e, "{\"volumenCargadoLitros\":10000}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nroViaje").value(1))
                .andExpect(jsonPath("$.estado").value("EN_REPARTO"))
                .andExpect(jsonPath("$.puntoLlenadoNombre").isNotEmpty())
                .andExpect(jsonPath("$.salida").isNotEmpty()));

        Parada p1 = e.paradas().get(0);
        enviarPosicion(e, p1.latitud(), p1.longitud()).andExpect(status().isNoContent());
        atender(e, p1).andExpect(status().isOk()).andExpect(jsonPath("$.viajeId").value((int) viaje1));

        finalizarViaje(e, viaje1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FINALIZADO"))
                .andExpect(jsonPath("$.retorno").isNotEmpty())
                .andExpect(jsonPath("$.paradasAtendidas").value(1));

        long viaje2 = idViaje(iniciarViaje(e, "{\"volumenCargadoLitros\":6000}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nroViaje").value(2)));
        Parada p2 = e.paradas().get(1);
        enviarPosicion(e, p2.latitud(), p2.longitud()).andExpect(status().isNoContent());
        atender(e, p2).andExpect(status().isOk()).andExpect(jsonPath("$.viajeId").value((int) viaje2));

        mockMvc.perform(conToken(e.tokenConductor(), patch("/api/conductor/rutas/" + e.rutaId() + "/finalizar")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("VIAJE_SIN_FINALIZAR"));
        finalizarViaje(e, viaje2).andExpect(status().isOk());
        mockMvc.perform(conToken(e.tokenConductor(), patch("/api/conductor/rutas/" + e.rutaId() + "/finalizar")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruta.estado").value("COMPLETADA"));

        mockMvc.perform(conToken(login("10000001", "Admin2026!"), get("/api/admin/rutas/" + e.rutaId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viajes.length()").value(2))
                .andExpect(jsonPath("$.viajes[0].volumenCargadoLitros").value(10000))
                .andExpect(jsonPath("$.viajes[1].paradasAtendidas").value(1))
                .andExpect(jsonPath("$.paradas[*].viajeId", Matchers.contains((int) viaje1, (int) viaje2)));
    }

    @Test
    void soloUnViajeAbiertoYSoloConLaRutaEnCurso() throws Exception {
        Escenario e = escenario();
        iniciarViaje(e, "{\"volumenCargadoLitros\":5000}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"));

        iniciar(e).andExpect(status().isOk());
        long viaje = idViaje(iniciarViaje(e, "{\"volumenCargadoLitros\":5000}").andExpect(status().isCreated()));
        iniciarViaje(e, "{\"volumenCargadoLitros\":5000}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("VIAJE_EN_CURSO"));

        finalizarViaje(e, viaje).andExpect(status().isOk());
        finalizarViaje(e, viaje)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"));
    }

    @Test
    void volumenNoPuedeSuperarLaCapacidadDeLaCisterna() throws Exception {
        Escenario e = escenario();
        iniciar(e).andExpect(status().isOk());
        iniciarViaje(e, "{\"volumenCargadoLitros\":10001}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VOLUMEN_EXCEDE_CAPACIDAD"));
        iniciarViaje(e, "{}").andExpect(status().isBadRequest());
    }

    @Test
    void puntoDeLlenadoInexistenteDevuelve404() throws Exception {
        Escenario e = escenario();
        iniciar(e).andExpect(status().isOk());
        iniciarViaje(e, "{\"puntoLlenadoId\":999999,\"volumenCargadoLitros\":5000}")
                .andExpect(status().isNotFound());
    }

    @Test
    void otroConductorNoOperaLosViajes() throws Exception {
        Escenario e = escenario();
        iniciar(e).andExpect(status().isOk());
        long viaje = idViaje(iniciarViaje(e, "{\"volumenCargadoLitros\":5000}").andExpect(status().isCreated()));

        String otro = login("20000002", "Conductor2026!");
        mockMvc.perform(conToken(otro, post("/api/conductor/rutas/" + e.rutaId() + "/viajes"))
                        .content("{\"volumenCargadoLitros\":5000}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(conToken(otro, patch("/api/conductor/viajes/" + viaje + "/finalizar")))
                .andExpect(status().isNotFound());
    }

    @Test
    void interrumpirLaRutaCierraElViajeAbierto() throws Exception {
        Escenario e = escenario();
        iniciar(e).andExpect(status().isOk());
        long viaje = idViaje(iniciarViaje(e, "{\"volumenCargadoLitros\":5000}").andExpect(status().isCreated()));

        mockMvc.perform(conToken(e.tokenConductor(), post("/api/incidencias"))
                        .content("{\"rutaId\":%d,\"tipo\":\"AVERIA\",\"detieneRuta\":true}".formatted(e.rutaId())))
                .andExpect(status().isCreated());

        mockMvc.perform(conToken(login("10000001", "Admin2026!"), get("/api/admin/rutas/" + e.rutaId())))
                .andExpect(jsonPath("$.ruta.estado").value("INTERRUMPIDA"))
                .andExpect(jsonPath("$.viajes[0].id").value((int) viaje))
                .andExpect(jsonPath("$.viajes[0].estado").value("FINALIZADO"));
    }

    // ---------------------------------------------------------------- helpers

    private ResultActions iniciarViaje(Escenario e, String body) throws Exception {
        return mockMvc.perform(conToken(e.tokenConductor(), post("/api/conductor/rutas/" + e.rutaId() + "/viajes"))
                .content(body));
    }

    private ResultActions finalizarViaje(Escenario e, long viajeId) throws Exception {
        return mockMvc.perform(conToken(e.tokenConductor(), patch("/api/conductor/viajes/" + viajeId + "/finalizar")));
    }

    private ResultActions atender(Escenario e, Parada p) throws Exception {
        return mockMvc.perform(conToken(e.tokenConductor(), patch("/api/conductor/paradas/" + p.id() + "/atender")));
    }

    private static long idViaje(ResultActions r) throws Exception {
        return ((Number) JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }
}
