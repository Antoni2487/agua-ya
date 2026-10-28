package pe.epsel.aguaya;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Comparator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import pe.epsel.aguaya.common.util.Haversine;
import pe.epsel.aguaya.persona.repository.VecinoRepository;
import pe.epsel.aguaya.sector.entity.Sector;
import pe.epsel.aguaya.sector.repository.SectorRepository;

/**
 * Verifica que V3__datos_referenciales.sql deja los datos que necesitan las demos y las reglas de negocio.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class DatosReferencialesTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SectorRepository sectorRepository;

    @Autowired
    private VecinoRepository vecinoRepository;

    private int contar(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }

    /** Filtra las filas de V3: el contenedor de BD es compartido y otros tests crean catálogos propios. */
    private static final String SECTORES_SEMILLA = "('Cerropón', 'P.J. Atusparias', 'P.J. Urrunaga', 'Callanca', 'Valle Hermoso')";

    @Test
    void cantidadesSegunCatalogo() {
        assertThat(contar("SELECT count(*) FROM sector WHERE nombre IN " + SECTORES_SEMILLA)).isEqualTo(5);
        assertThat(contar("SELECT count(*) FROM sector WHERE nombre IN " + SECTORES_SEMILLA
                + " AND dotacion_litros <> 80")).isZero();
        assertThat(contar("SELECT count(*) FROM punto_llenado WHERE nombre LIKE '%EPSEL (referencial)'")).isEqualTo(1);
        assertThat(contar("SELECT count(*) FROM cisterna WHERE placa LIKE 'AYA-%'")).isEqualTo(5);
        assertThat(contar("SELECT count(*) FROM administrador WHERE persona_id IN "
                + "(SELECT id FROM persona WHERE dni LIKE '1000%')")).isEqualTo(1);
        assertThat(contar("SELECT count(*) FROM conductor WHERE persona_id IN "
                + "(SELECT id FROM persona WHERE dni LIKE '2000%')")).isEqualTo(3);
        assertThat(contar("SELECT count(*) FROM vecino WHERE persona_id IN (SELECT id FROM persona WHERE dni LIKE '3000%')"))
                .isEqualTo(12);
    }

    @Test
    void cadaSectorTieneAlMenosUnaInstitucionCritica() {
        assertThat(contar("SELECT count(*) FROM sector s WHERE s.nombre IN " + SECTORES_SEMILLA + " AND NOT EXISTS "
                + "(SELECT 1 FROM institucion_critica i WHERE i.sector_id = s.id)")).isZero();
    }

    @Test
    void hayCisternaConAutorizacionVencidaYOtraEnMantenimiento() {
        assertThat(jdbc.queryForObject("SELECT vence_autorizacion FROM cisterna WHERE placa = 'AYA-104'",
                LocalDate.class)).isBefore(LocalDate.of(2026, 1, 1));
        assertThat(jdbc.queryForObject("SELECT estado_vehiculo FROM cisterna WHERE placa = 'AYA-105'",
                String.class)).isEqualTo("MANTENIMIENTO");
    }

    /** Regla 8: el sector de cada vecino semilla es el más cercano a su pin. */
    @Test
    @Transactional
    void cadaVecinoEstaEnSuSectorMasCercano() {
        var sectores = sectorRepository.findByActivoTrue();
        vecinoRepository.findAll().stream()
                .filter(v -> v.getDni().startsWith("3000"))
                .forEach(v -> {
                    Sector masCercano = sectores.stream()
                            .min(Comparator.comparingDouble(s -> Haversine.distanciaMetros(
                                    v.getLatitud().doubleValue(), v.getLongitud().doubleValue(),
                                    s.getLatCentro().doubleValue(), s.getLngCentro().doubleValue())))
                            .orElseThrow();
                    assertThat(v.getSector().getId()).as("sector del vecino %s", v.getDni())
                            .isEqualTo(masCercano.getId());
                });
    }

    @ParameterizedTest
    @CsvSource({
            "10000001, Admin2026!, ADMIN",
            "20000001, Conductor2026!, CONDUCTOR",
            "30000001, Vecino2026!, VECINO"
    })
    void lasCuentasDePruebaPuedenIngresar(String dni, String password, String rol) throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dni\":\"%s\",\"password\":\"%s\"}".formatted(dni, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.rol").value(rol));
    }
}
