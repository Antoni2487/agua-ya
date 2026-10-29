package pe.epsel.aguaya.transparencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import pe.epsel.aguaya.TestcontainersConfig;

/**
 * Con la caché activa, repetir una consulta del panel público no vuelve a la base de datos.
 */
@SpringBootTest(properties = "spring.cache.type=caffeine")
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class CacheTransparenciaTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManagerFactory emf;

    @Test
    void laSegundaConsultaSaleDeLaCache() throws Exception {
        Statistics estadisticas = emf.unwrap(SessionFactory.class).getStatistics();
        for (String ruta : new String[] {"/resumen", "/sectores?distrito=Monsefú", "/cronograma"}) {
            mockMvc.perform(get("/api/public/transparencia" + ruta)).andExpect(status().isOk());

            estadisticas.clear();
            mockMvc.perform(get("/api/public/transparencia" + ruta)).andExpect(status().isOk());
            assertThat(estadisticas.getPrepareStatementCount()).as(ruta).isZero();
        }
        // Otros parámetros son otra entrada de la caché.
        estadisticas.clear();
        mockMvc.perform(get("/api/public/transparencia/sectores?distrito=Chiclayo")).andExpect(status().isOk());
        assertThat(estadisticas.getPrepareStatementCount()).isPositive();
    }
}
