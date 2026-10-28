package pe.epsel.aguaya.calificacion;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.ResultActions;

import pe.epsel.aguaya.TestcontainersConfig;
import pe.epsel.aguaya.soporte.EscenarioRutaHoy;

/**
 * Calificaciones por parada atendida (RF08, regla 7).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class CalificacionesIntegrationTest extends EscenarioRutaHoy {

    @Test
    void losVecinosDelPuntoCalificanUnaVezYElAdminVeElResumen() throws Exception {
        Escenario e = escenario();
        String vecina = vecinoDelSector(e);
        String vecino = vecinoDelSector(e);
        Parada punto = atenderPrimera(e);

        calificar(vecina, punto.id(), 5, "  Llegó a tiempo  ")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estrellas").value(5))
                .andExpect(jsonPath("$.comentario").value("Llegó a tiempo"))
                .andExpect(jsonPath("$.rutaId").value((int) e.rutaId()))
                .andExpect(jsonPath("$.cisternaPlaca").isNotEmpty());
        calificar(vecina, punto.id(), 4, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CALIFICACION_DUPLICADA"));
        // Otro vecino del mismo punto de entrega también califica (una vez por parada y vecino).
        calificar(vecino, punto.id(), 3, "").andExpect(status().isCreated())
                .andExpect(jsonPath("$.comentario").doesNotExist());

        mockMvc.perform(conToken(vecina, get("/api/vecino/proxima-atencion")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proxima.miParada.miCalificacion").value(5));

        String admin = login("10000001", "Admin2026!");
        mockMvc.perform(conToken(admin, get("/api/admin/calificaciones?sectorId=" + e.sectorId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.contenido[0].estrellas").value(3));
        mockMvc.perform(conToken(admin, get("/api/admin/calificaciones/resumen?sectorId=" + e.sectorId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.promedio").value(4.0))
                .andExpect(jsonPath("$.distribucion.5").value(1))
                .andExpect(jsonPath("$.distribucion.3").value(1))
                .andExpect(jsonPath("$.distribucion.1").value(0))
                .andExpect(jsonPath("$.conductores.length()").value(1))
                .andExpect(jsonPath("$.conductores[0].conductorId").value((int) e.conductorId()))
                .andExpect(jsonPath("$.conductores[0].promedio").value(4.0));
    }

    @Test
    void soloSuParadaYSoloAtendida() throws Exception {
        Escenario e = escenario();
        String vecino = vecinoDelSector(e);
        Parada suya = e.paradas().get(0);
        Parada otra = e.paradas().get(1);

        iniciar(e).andExpect(status().isOk());
        calificar(vecino, suya.id(), 4, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PARADA_NO_ATENDIDA"));
        // El punto más cercano a su vivienda es el primero: el segundo no es suyo.
        calificar(vecino, otra.id(), 4, null).andExpect(status().isNotFound());
        calificar(login("30000004", "Vecino2026!"), suya.id(), 4, null).andExpect(status().isNotFound());
        calificar(vecino, 999_999, 4, null).andExpect(status().isNotFound());
    }

    @Test
    void validaEstrellasYRol() throws Exception {
        Escenario e = escenario();
        String vecino = vecinoDelSector(e);
        Parada punto = atenderPrimera(e);

        calificar(vecino, punto.id(), 6, null).andExpect(status().isBadRequest());
        calificar(vecino, punto.id(), 0, null).andExpect(status().isBadRequest());
        calificar(vecino, punto.id(), 4, "x".repeat(501)).andExpect(status().isBadRequest());
        calificar(e.tokenConductor(), punto.id(), 4, null).andExpect(status().isForbidden());
        mockMvc.perform(conToken(vecino, get("/api/admin/calificaciones"))).andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- helpers

    private Parada atenderPrimera(Escenario e) throws Exception {
        Parada p = e.paradas().get(0);
        iniciar(e).andExpect(status().isOk());
        enviarPosicion(e, p.latitud(), p.longitud()).andExpect(status().isNoContent());
        mockMvc.perform(conToken(e.tokenConductor(), patch("/api/conductor/paradas/" + p.id() + "/atender")))
                .andExpect(status().isOk());
        return p;
    }

    private ResultActions calificar(String token, long paradaId, int estrellas, String comentario) throws Exception {
        String texto = comentario == null ? "null" : "\"" + comentario + "\"";
        return mockMvc.perform(conToken(token, post("/api/vecino/calificaciones")).content(
                "{\"paradaId\":%d,\"estrellas\":%d,\"comentario\":%s}".formatted(paradaId, estrellas, texto)));
    }
}
