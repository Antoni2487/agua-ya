package pe.epsel.aguaya.rendimiento;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManagerFactory;
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

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class MedicionTemporalTest extends EscenarioRutaHoy {

    @Autowired
    EntityManagerFactory emf;

    @Test
    void medir() throws Exception {
        Statistics st = emf.unwrap(SessionFactory.class).getStatistics();
        String admin = login("10000001", "Admin2026!");
        Escenario[] es = {escenario(), escenario(), escenario()};
        String vecino = null;
        for (Escenario e : es) {
            vecino = vecinoDelSector(e);
            iniciar(e).andExpect(status().isOk());
            for (int i = 0; i < 3; i++) {
                enviarPosicion(e, e.paradas().get(0).latitud() + 0.01, e.paradas().get(0).longitud());
            }
            mockMvc.perform(conToken(e.tokenConductor(), patch("/api/conductor/paradas/" + e.paradas().get(1).id() + "/atender")));
            mockMvc.perform(conToken(e.tokenConductor(), post("/api/incidencias")).content("{\"rutaId\":%d,\"tipo\":\"OTRO\"}".formatted(e.rutaId())));
        }
        Escenario e = es[2];
        String[][] casos = {
                {admin, "/api/admin/rutas"}, {admin, "/api/admin/rutas/" + e.rutaId()},
                {admin, "/api/admin/rutas/" + e.rutaId() + "/recorrido"},
                {admin, "/api/admin/rutas/" + e.rutaId() + "/sugerencias-asignacion"},
                {admin, "/api/admin/incidencias"}, {e.tokenConductor(), "/api/incidencias"},
                {admin, "/api/admin/calificaciones"}, {admin, "/api/admin/calificaciones/resumen"},
                {admin, "/api/admin/sectores"}, {admin, "/api/admin/instituciones"}, {admin, "/api/admin/puntos-entrega"},
                {admin, "/api/admin/puntos-llenado"}, {admin, "/api/admin/cisternas"}, {admin, "/api/admin/conductores"},
                {admin, "/api/admin/capacidad"},
                {e.tokenConductor(), "/api/conductor/rutas/hoy"}, {vecino, "/api/vecino/proxima-atencion"},
                {vecino, "/api/vecino/me"}, {vecino, "/api/vecino/rutas/" + e.rutaId() + "/posicion"},
                {null, "/api/public/transparencia/resumen"}, {null, "/api/public/transparencia/sectores"},
                {null, "/api/public/transparencia/cronograma"},
        };
        StringBuilder sb = new StringBuilder("\nMEDICION\n");
        for (String[] c : casos) {
            MockHttpServletRequestBuilder r = get(c[1]);
            if (c[0] != null) r = conToken(c[0], r);
            st.clear();
            int code = mockMvc.perform(r).andReturn().getResponse().getStatus();
            sb.append("%-60s %d  sql=%d%n".formatted(c[1], code, st.getPrepareStatementCount()));
        }
        st.clear();
        enviarPosicion(e, e.paradas().get(0).latitud(), e.paradas().get(0).longitud());
        Thread.sleep(500);
        sb.append("%-60s sql=%d%n".formatted("POST posicion (con aviso cercania)", st.getPrepareStatementCount()));
        st.clear();
        enviarPosicion(e, e.paradas().get(0).latitud(), e.paradas().get(0).longitud());
        Thread.sleep(500);
        sb.append("%-60s sql=%d%n".formatted("POST posicion (sin aviso)", st.getPrepareStatementCount()));
        System.out.println(sb);
    }
}
