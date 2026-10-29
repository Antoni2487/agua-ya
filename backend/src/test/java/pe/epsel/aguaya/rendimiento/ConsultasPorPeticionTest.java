package pe.epsel.aguaya.rendimiento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EntityManagerFactory;
import org.assertj.core.api.SoftAssertions;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import pe.epsel.aguaya.TestcontainersConfig;
import pe.epsel.aguaya.soporte.EscenarioRutaHoy;

/**
 * Guarda contra N+1: con datos de varios sectores, conductores, cisternas e incidencias, cada endpoint
 * de lectura ejecuta un número fijo de sentencias SQL (no crece con la cantidad de filas). Si alguien agrega
 * una relación lazy a un mapper sin {@code @EntityGraph}, este test falla. En los listados el límite incluye
 * el COUNT de la paginación, que Spring omite cuando hay menos filas que el tamaño de página.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class ConsultasPorPeticionTest extends EscenarioRutaHoy {

    @Autowired
    private EntityManagerFactory emf;

    private record Caso(String token, String url, int maxSentencias) {
    }

    @Test
    void cadaLecturaEjecutaUnNumeroFijoDeConsultas() throws Exception {
        String admin = login("10000001", "Admin2026!");
        List<Escenario> escenarios = new ArrayList<>();
        String vecino = null;
        for (int i = 0; i < 3; i++) {
            Escenario e = escenario();
            escenarios.add(e);
            vecino = vecinoDelSector(e);
            Parada p = e.paradas().get(0);
            iniciar(e).andExpect(status().isOk());
            enviarPosicion(e, p.latitud(), p.longitud()).andExpect(status().isNoContent());
            mockMvc.perform(conToken(e.tokenConductor(), patch("/api/conductor/paradas/" + p.id() + "/atender")))
                    .andExpect(status().isOk());
            mockMvc.perform(conToken(e.tokenConductor(), patch("/api/conductor/paradas/"
                            + e.paradas().get(1).id() + "/atender")))
                    .andExpect(status().isConflict()); // intento fuera de geocerca para el recorrido
            mockMvc.perform(conToken(e.tokenConductor(), post("/api/incidencias"))
                            .content("{\"rutaId\":%d,\"tipo\":\"OTRO\"}".formatted(e.rutaId())))
                    .andExpect(status().isCreated());
            mockMvc.perform(conToken(vecino, post("/api/vecino/calificaciones"))
                            .content("{\"paradaId\":%d,\"estrellas\":4}".formatted(p.id())))
                    .andExpect(status().isCreated());
        }
        Escenario e = escenarios.get(2);
        Thread.sleep(500); // deja terminar los avisos push en segundo plano antes de medir

        List<Caso> casos = List.of(
                new Caso(admin, "/api/admin/rutas", 3), // página + COUNT + conteo de paradas agregado
                new Caso(admin, "/api/admin/rutas/" + e.rutaId(), 3),
                new Caso(admin, "/api/admin/rutas/" + e.rutaId() + "/recorrido", 3),
                new Caso(admin, "/api/admin/incidencias", 2),
                new Caso(e.tokenConductor(), "/api/incidencias", 2),
                new Caso(admin, "/api/admin/calificaciones", 2),
                new Caso(admin, "/api/admin/calificaciones/resumen", 2),
                new Caso(admin, "/api/admin/sectores", 2),
                new Caso(admin, "/api/admin/instituciones", 2),
                new Caso(admin, "/api/admin/puntos-entrega", 2),
                new Caso(admin, "/api/admin/cisternas", 2),
                new Caso(admin, "/api/admin/conductores", 2),
                new Caso(admin, "/api/admin/capacidad", 2),
                new Caso(e.tokenConductor(), "/api/conductor/rutas/hoy", 3),
                new Caso(vecino, "/api/vecino/me", 1),
                new Caso(vecino, "/api/vecino/proxima-atencion", 5),
                new Caso(vecino, "/api/vecino/rutas/" + e.rutaId() + "/posicion", 3),
                new Caso(null, "/api/public/transparencia/resumen", 7),
                new Caso(null, "/api/public/transparencia/sectores", 5),
                new Caso(null, "/api/public/transparencia/cronograma", 2));

        Statistics estadisticas = emf.unwrap(SessionFactory.class).getStatistics();
        SoftAssertions afirmaciones = new SoftAssertions();
        for (Caso c : casos) {
            MockHttpServletRequestBuilder peticion = c.token() != null ? conToken(c.token(), get(c.url())) : get(c.url());
            estadisticas.clear();
            mockMvc.perform(peticion).andExpect(status().isOk());
            afirmaciones.assertThat(estadisticas.getPrepareStatementCount()).as(c.url()).isLessThanOrEqualTo(c.maxSentencias());
        }
        afirmaciones.assertAll();
        assertThat(casos).isNotEmpty();
    }
}
