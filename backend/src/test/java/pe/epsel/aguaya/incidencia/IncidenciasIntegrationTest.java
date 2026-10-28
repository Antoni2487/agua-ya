package pe.epsel.aguaya.incidencia;

import static org.assertj.core.api.Assertions.assertThat;
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
 * Reporte de incidencias (RF06), interrupción de ruta (regla 5) y próxima atención del vecino.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class IncidenciasIntegrationTest extends EscenarioRutaHoy {

    // ------------------------------------------------------------ conductor

    @Test
    void conductorReportaYElAdminLaCierra() throws Exception {
        Escenario e = escenario();
        long parada = e.paradas().get(1).id();

        String body = reportar(e.tokenConductor(), """
                {"rutaId":%d,"paradaId":%d,"tipo":"ACCESO_BLOQUEADO","descripcion":"  Calle con zanja  "}"""
                .formatted(e.rutaId(), parada))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ABIERTA"))
                .andExpect(jsonPath("$.paradaId").value((int) parada))
                .andExpect(jsonPath("$.ordenParada").value(2))
                .andExpect(jsonPath("$.descripcion").value("Calle con zanja"))
                .andExpect(jsonPath("$.reportadoPorRol").value("CONDUCTOR"))
                .andExpect(jsonPath("$.estadoRuta").value("PROGRAMADA"))
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(body, "$.id");

        String admin = login("10000001", "Admin2026!");
        mockMvc.perform(conToken(admin, get("/api/admin/incidencias?estado=ABIERTA&rutaId=" + e.rutaId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[*].id").value(Matchers.contains(id)));

        mockMvc.perform(conToken(admin, patch("/api/admin/incidencias/" + id + "/cerrar")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADA"))
                .andExpect(jsonPath("$.resueltaPorNombre").value("Admin EPSEL (referencial)"))
                .andExpect(jsonPath("$.resueltoEn").exists());
        mockMvc.perform(conToken(admin, patch("/api/admin/incidencias/" + id + "/cerrar")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("INCIDENCIA_CERRADA"));

        // Cerrada, se puede volver a reportar el mismo tipo
        reportar(e.tokenConductor(), """
                {"rutaId":%d,"tipo":"ACCESO_BLOQUEADO"}""".formatted(e.rutaId()))
                .andExpect(status().isCreated());
    }

    @Test
    void conductorNoUsaTiposDeVecinoNiReportaRutasAjenas() throws Exception {
        Escenario e = escenario();
        reportar(e.tokenConductor(), """
                {"rutaId":%d,"tipo":"NO_LLEGO"}""".formatted(e.rutaId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("TIPO_NO_PERMITIDO"));

        Escenario otra = escenario();
        reportar(e.tokenConductor(), """
                {"rutaId":%d,"tipo":"AVERIA"}""".formatted(otra.rutaId()))
                .andExpect(status().isNotFound());
        reportar(e.tokenConductor(), """
                {"rutaId":%d,"paradaId":%d,"tipo":"OTRO"}""".formatted(e.rutaId(), otra.paradas().get(0).id()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PARADA_NO_PERTENECE"));
    }

    @Test
    void averiaQueDetieneLaRutaLaInterrumpe() throws Exception {
        Escenario e = escenario();
        reportar(e.tokenConductor(), """
                {"rutaId":%d,"tipo":"AVERIA","detieneRuta":true}""".formatted(e.rutaId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"));

        iniciar(e).andExpect(status().isOk());
        reportar(e.tokenConductor(), """
                {"rutaId":%d,"tipo":"AVERIA","descripcion":"Llanta reventada","detieneRuta":true}""".formatted(e.rutaId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoRuta").value("INTERRUMPIDA"));

        assertThat(jdbc.queryForObject("SELECT estado_disponibilidad FROM conductor WHERE persona_id = ?", String.class,
                e.conductorId())).isEqualTo("DISPONIBLE");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM parada WHERE ruta_id = ? AND estado = 'PENDIENTE'",
                Integer.class, e.rutaId())).isEqualTo(2);
        enviarPosicion(e, -6.3, -79.4)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"));
    }

    // --------------------------------------------------------------- vecino

    @Test
    void vecinoVeSuProximaAtencionYReporta() throws Exception {
        Escenario e = escenario();
        String vecino = vecinoDelSector(e);

        mockMvc.perform(conToken(vecino, get("/api/vecino/proxima-atencion")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proxima.rutaId").value((int) e.rutaId()))
                .andExpect(jsonPath("$.proxima.estado").value("PROGRAMADA"))
                .andExpect(jsonPath("$.proxima.totalParadas").value(2))
                .andExpect(jsonPath("$.proxima.miParada.paradaId").value((int) e.paradas().get(0).id()))
                .andExpect(jsonPath("$.proxima.miParada.tipo").value("PUNTO_ENTREGA"))
                .andExpect(jsonPath("$.ultima").doesNotExist());

        reportar(vecino, """
                {"rutaId":%d,"paradaId":%d,"tipo":"NO_LLEGO"}""".formatted(e.rutaId(), e.paradas().get(0).id()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reportadoPorRol").value("VECINO"));
        reportar(vecino, """
                {"rutaId":%d,"tipo":"NO_LLEGO"}""".formatted(e.rutaId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("INCIDENCIA_DUPLICADA"));

        mockMvc.perform(conToken(vecino, get("/api/incidencias")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.contenido[0].tipo").value("NO_LLEGO"));
    }

    @Test
    void vecinoNoUsaTiposDelConductorNiDetieneRutasNiReportaOtrosSectores() throws Exception {
        Escenario e = escenario();
        String vecino = vecinoDelSector(e);
        reportar(vecino, """
                {"rutaId":%d,"tipo":"AVERIA"}""".formatted(e.rutaId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("TIPO_NO_PERMITIDO"));
        reportar(vecino, """
                {"rutaId":%d,"tipo":"OTRO","detieneRuta":true}""".formatted(e.rutaId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DETENER_NO_PERMITIDO"));

        String vecinoOtroSector = login("30000004", "Vecino2026!");
        reportar(vecinoOtroSector, """
                {"rutaId":%d,"tipo":"NO_LLEGO"}""".formatted(e.rutaId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void laUltimaAtencionAparecealCompletarLaRuta() throws Exception {
        Escenario e = escenario();
        String vecino = vecinoDelSector(e);
        iniciar(e).andExpect(status().isOk());
        for (Parada p : e.paradas()) {
            enviarPosicion(e, p.latitud(), p.longitud()).andExpect(status().isNoContent());
            mockMvc.perform(conToken(e.tokenConductor(), patch("/api/conductor/paradas/" + p.id() + "/atender")))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(conToken(e.tokenConductor(), patch("/api/conductor/rutas/" + e.rutaId() + "/finalizar")))
                .andExpect(status().isOk());

        mockMvc.perform(conToken(vecino, get("/api/vecino/proxima-atencion")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proxima").doesNotExist())
                .andExpect(jsonPath("$.ultima.rutaId").value((int) e.rutaId()))
                .andExpect(jsonPath("$.ultima.estado").value("COMPLETADA"))
                .andExpect(jsonPath("$.ultima.paradasAtendidas").value(2))
                .andExpect(jsonPath("$.ultima.miParada.estado").value("ATENDIDA"));
    }

    @Test
    void elAdminNoReportaIncidencias() throws Exception {
        Escenario e = escenario();
        reportar(login("10000001", "Admin2026!"), """
                {"rutaId":%d,"tipo":"OTRO"}""".formatted(e.rutaId()))
                .andExpect(status().isForbidden());
    }

    private ResultActions reportar(String token, String json) throws Exception {
        return mockMvc.perform(conToken(token, post("/api/incidencias")).content(json));
    }
}
