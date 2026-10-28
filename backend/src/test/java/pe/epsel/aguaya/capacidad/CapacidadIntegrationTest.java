package pe.epsel.aguaya.capacidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import com.jayway.jsonpath.JsonPath;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import pe.epsel.aguaya.TestcontainersConfig;

/**
 * Planificación de capacidad (RF02). Otros tests agregan cisternas a la misma BD, así que la flota
 * esperada se calcula con SQL en vez de fijarla.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class CapacidadIntegrationTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 11, 2);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private String admin;

    @BeforeEach
    void login() throws Exception {
        admin = token("10000001", "Admin2026!");
    }

    @Test
    void sectorConCapacidadSimulada() throws Exception {
        // P.J. Urrunaga: 280 UU × 80 L × 2 días = 44 800 L ÷ 10 000 L -> 5 viajes; máx. 3 al día -> 2 cisternas
        long urrunaga = idSector("P.J. Urrunaga");
        int disponibles = disponiblesEn(FECHA);

        capacidad("?sectorId=%d&fecha=%s&capacidadLitros=10000".formatted(urrunaga, FECHA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fecha").value(FECHA.toString()))
                .andExpect(jsonPath("$.capacidadReferenciaLitros").value(10000))
                .andExpect(jsonPath("$.sectores.length()").value(1))
                .andExpect(jsonPath("$.sectores[0].litrosPorVisita").value(44800))
                .andExpect(jsonPath("$.sectores[0].viajesNecesarios").value(5))
                .andExpect(jsonPath("$.sectores[0].maxViajesDiarios").value(3))
                .andExpect(jsonPath("$.sectores[0].cisternasNecesarias").value(2))
                .andExpect(jsonPath("$.cisternasNecesarias").value(2))
                .andExpect(jsonPath("$.cisternasDisponibles").value(disponibles))
                .andExpect(jsonPath("$.deficit").value(Math.max(0, 2 - disponibles)));
    }

    @Test
    void laCapacidadPorDefectoEsElPromedioDeLasDisponibles() throws Exception {
        int promedio = jdbc.queryForObject("SELECT floor(avg(capacidad_litros)) FROM cisterna "
                + "WHERE estado_vehiculo = 'OPERATIVA' AND vence_autorizacion >= ?", Integer.class, FECHA);
        capacidad("?sectorId=%d&fecha=%s".formatted(idSector("Cerropón"), FECHA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capacidadReferenciaLitros").value(promedio));
    }

    @Test
    void laAutorizacionVencidaYElMantenimientoNoCuentan() throws Exception {
        // AYA-104 vence el 2025-12-31 y AYA-105 está en mantenimiento.
        String body = capacidad("?fecha=" + FECHA).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int total = JsonPath.read(body, "$.flota.total");
        int disponibles = JsonPath.read(body, "$.flota.disponibles");
        int noOperativas = JsonPath.read(body, "$.flota.noOperativas");
        int vencidas = JsonPath.read(body, "$.flota.autorizacionVencida");
        assertThat(total).isEqualTo(jdbc.queryForObject("SELECT count(*) FROM cisterna", Integer.class));
        assertThat(disponibles).isEqualTo(disponiblesEn(FECHA));
        assertThat(noOperativas).isGreaterThanOrEqualTo(1);
        assertThat(vencidas).isGreaterThanOrEqualTo(1);
        assertThat(disponibles + noOperativas + vencidas).isEqualTo(total);
    }

    @Test
    void sinSectorSumaTodosLosSectoresActivosYMarcaElDeficit() throws Exception {
        // En 2100 todas las autorizaciones están vencidas: toda la demanda es déficit.
        String body = capacidad("?fecha=2100-01-01&capacidadLitros=10000").andExpect(status().isOk())
                .andExpect(jsonPath("$.cisternasDisponibles").value(0))
                .andExpect(jsonPath("$.hayDeficit").value(true))
                .andExpect(jsonPath("$.sectores[*].nombre", Matchers.hasItems("Cerropón", "P.J. Atusparias",
                        "P.J. Urrunaga", "Callanca", "Valle Hermoso")))
                .andReturn().getResponse().getContentAsString();
        List<Integer> porSector = JsonPath.read(body, "$.sectores[*].cisternasNecesarias");
        int necesarias = JsonPath.read(body, "$.cisternasNecesarias");
        assertThat(necesarias).isEqualTo(porSector.stream().mapToInt(Integer::intValue).sum());
        assertThat((int) JsonPath.read(body, "$.deficit")).isEqualTo(necesarias);
        assertThat(porSector).hasSize(jdbc.queryForObject("SELECT count(*) FROM sector WHERE activo", Integer.class));
    }

    @Test
    void validaParametrosYRol() throws Exception {
        capacidad("?sectorId=999999").andExpect(status().isNotFound());
        capacidad("?capacidadLitros=0")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CAPACIDAD_INVALIDA"));
        capacidad("?fecha=no-es-fecha").andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/capacidad")
                        .header("Authorization", "Bearer " + token("30000001", "Vecino2026!")))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- helpers

    private ResultActions capacidad(String query) throws Exception {
        return mockMvc.perform(get("/api/admin/capacidad" + query).header("Authorization", "Bearer " + admin));
    }

    private long idSector(String nombre) {
        return jdbc.queryForObject("SELECT id FROM sector WHERE nombre = ?", Long.class, nombre);
    }

    private int disponiblesEn(LocalDate fecha) {
        return jdbc.queryForObject("SELECT count(*) FROM cisterna WHERE estado_vehiculo = 'OPERATIVA' "
                + "AND vence_autorizacion >= ?", Integer.class, fecha);
    }

    private String token(String dni, String password) throws Exception {
        return JsonPath.read(mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dni\":\"%s\",\"password\":\"%s\"}".formatted(dni, password)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.accessToken");
    }
}
