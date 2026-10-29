package pe.epsel.aguaya;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class AguayaApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayCreaTodasLasTablasDelModelo() {
        List<String> tablas = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables "
                        + "WHERE table_schema = 'public' AND table_name <> 'flyway_schema_history'",
                String.class);

        assertThat(tablas).containsExactlyInAnyOrder(
                "persona", "vecino", "conductor", "administrador",
                "codigo_otp", "refresh_token", "dispositivo_push",
                "sector", "institucion_critica", "punto_llenado", "punto_entrega", "cisterna",
                "ruta", "viaje", "parada", "monitoreo_gps", "intento_geocerca",
                "incidencia", "calificacion_servicio");
    }

    @Test
    void lasClavesForaneasConsultadasTienenIndice() {
        List<String> indices = jdbcTemplate.queryForList(
                "SELECT indexname FROM pg_indexes WHERE schemaname = 'public'", String.class);

        assertThat(indices).contains("ix_incidencia_reportado_ruta", "ix_incidencia_creado", "ix_parada_viaje",
                "ix_parada_punto_entrega", "ix_parada_institucion", "ix_ruta_punto_llenado", "ix_viaje_punto_llenado",
                "ix_monitoreo_ruta_registrado", "ix_parada_ruta_orden");
    }

    @Test
    void healthEsPublicoYReportaBaseDeDatos() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("UP"))
                .andExpect(jsonPath("$.baseDatos").value("UP"));
    }

    @Test
    void swaggerEstaDisponible() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("AguaYa API"));
    }

    @Test
    void rutaProtegidaSinTokenDevuelve401ConFormatoUniforme() throws Exception {
        mockMvc.perform(get("/api/admin/sectores"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"))
                .andExpect(jsonPath("$.mensaje").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.detalles").isArray());
    }

    @Test
    @WithMockUser
    void recursoInexistenteDevuelve404ConFormatoUniforme() throws Exception {
        mockMvc.perform(get("/api/no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }

    @Test
    void corsPermiteElOrigenConfigurado() throws Exception {
        mockMvc.perform(options("/api/health")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
