package pe.epsel.aguaya;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Programación (RF01) y asignación (RF03) de rutas sobre los datos de V3/V4.
 * Cada test usa una fecha futura propia para no chocar con la regla "una ruta por sector, fecha y turno".
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class RutasIntegrationTest {

    private static final AtomicInteger DIAS = new AtomicInteger(30);
    private static final LocalDate HOY = LocalDate.now(ZoneId.of("America/Lima"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private String tokenAdmin;
    private long planta;

    @BeforeEach
    void setUp() throws Exception {
        tokenAdmin = JsonPath.read(mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dni\":\"10000001\",\"password\":\"Admin2026!\"}"))
                .andReturn().getResponse().getContentAsString(), "$.accessToken");
        planta = jdbc.queryForObject("SELECT min(id) FROM punto_llenado WHERE tipo = 'PLANTA'", Long.class);
    }

    // ------------------------------------------------------------- programar

    @Test
    void sectorDomicilioGeneraInstitucionesYHogaresConLasCriticasPrimero() throws Exception {
        long sector = sectorId("P.J. Atusparias");
        int instituciones = contar("SELECT count(*) FROM institucion_critica WHERE activo AND sector_id = ?", sector);
        int vecinos = contar("SELECT count(*) FROM vecino v JOIN persona p ON p.id = v.persona_id "
                + "WHERE p.activo AND v.sector_id = ?", sector);

        String body = programar(sector, nuevaFecha(), "MANANA")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ruta.estado").value("PROGRAMADA"))
                .andExpect(jsonPath("$.ruta.sectorNombre").value("P.J. Atusparias"))
                .andExpect(jsonPath("$.ruta.cisternaId").doesNotExist())
                .andExpect(jsonPath("$.ruta.totalParadas").value(instituciones + vecinos))
                .andExpect(jsonPath("$.puntoLlenadoId").value((int) planta))
                .andReturn().getResponse().getContentAsString();

        List<String> prioridades = JsonPath.read(body, "$.paradas[*].prioridad");
        List<String> tipos = JsonPath.read(body, "$.paradas[*].tipo");
        List<Integer> orden = JsonPath.read(body, "$.paradas[*].orden");
        assertThat(prioridades.subList(0, instituciones)).containsOnly("CRITICA");
        assertThat(prioridades.subList(instituciones, prioridades.size())).containsOnly("NORMAL");
        assertThat(tipos).containsOnly("INSTITUCION", "HOGAR");
        assertThat(orden).isEqualTo(java.util.stream.IntStream.rangeClosed(1, orden.size()).boxed().toList());
        assertThat((List<String>) JsonPath.read(body, "$.paradas[?(@.tipo=='HOGAR')].nombre")).allMatch(n -> !n.isBlank());
    }

    @Test
    void sectorConPuntosDeEntregaNoGeneraParadasPorHogar() throws Exception {
        long sector = sectorId("Cerropón");
        int instituciones = contar("SELECT count(*) FROM institucion_critica WHERE activo AND sector_id = ?", sector);
        int puntos = contar("SELECT count(*) FROM punto_entrega WHERE activo AND sector_id = ?", sector);
        assertThat(puntos).isEqualTo(2);

        programar(sector, nuevaFecha(), "TARDE")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ruta.totalParadas").value(instituciones + puntos))
                .andExpect(jsonPath("$.paradas[*].tipo").value(Matchers.everyItem(Matchers.oneOf("INSTITUCION", "PUNTO_ENTREGA"))))
                .andExpect(jsonPath("$.paradas[?(@.tipo=='PUNTO_ENTREGA')].referencia").value(Matchers.hasItem("Frente a la losa, parte baja")));
    }

    @Test
    void rutaDuplicadaYFechaPasadaSeRechazan() throws Exception {
        long sector = sectorId("Callanca");
        LocalDate fecha = nuevaFecha();
        programar(sector, fecha, "MANANA").andExpect(status().isCreated());

        programar(sector, fecha, "MANANA")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("RUTA_DUPLICADA"));
        programar(sector, fecha, "TARDE").andExpect(status().isCreated());

        programar(sector, HOY.minusDays(1), "MANANA")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("FECHA_PASADA"));
    }

    @Test
    void sectorSinParadasNoSePuedeProgramar() throws Exception {
        Integer vacio = JsonPath.read(perform(post("/api/admin/sectores").content("""
                {"nombre":"Sector vacío %d","distrito":"Monsefú","modalidad":"CONTINGENCIA","esZonaAlta":false,
                 "unidadesUso":50,"frecuenciaDias":1,"maxViajesDiarios":2,"latCentro":-6.9,"lngCentro":-79.9}"""
                .formatted(DIAS.incrementAndGet())))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");

        programar(vacio.longValue(), nuevaFecha(), "MANANA")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("RUTA_SIN_PARADAS"));
    }

    // ------------------------------------------------------------ sugerencias

    @Test
    void sugerenciasExcluyenCisternasNoAsignablesYOrdenanPorViajes() throws Exception {
        long ruta = rutaProgramada("Cerropón", nuevaFecha(), "MANANA");

        perform(get("/api/admin/rutas/" + ruta + "/sugerencias-asignacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.litrosPorVisita").value(28_000))
                .andExpect(jsonPath("$.maxViajesDiarios").value(4))
                .andExpect(jsonPath("$.cisternas[0].placa").value("AYA-103"))
                .andExpect(jsonPath("$.cisternas[0].viajesNecesarios").value(2))
                .andExpect(jsonPath("$.cisternas[?(@.placa=='AYA-101')].viajesNecesarios").value(3))
                .andExpect(jsonPath("$.cisternas[*].placa").value(Matchers.not(Matchers.hasItem("AYA-104"))))
                .andExpect(jsonPath("$.cisternas[*].placa").value(Matchers.not(Matchers.hasItem("AYA-105"))))
                .andExpect(jsonPath("$.conductores[0].mismoTurno").value(true))
                .andExpect(jsonPath("$.conductores[*].id").value(Matchers.hasItem((int) conductor("20000003"))));
    }

    // -------------------------------------------------------------- asignar

    @Test
    void asignarAplicaLaRegla2YCalculaLosViajes() throws Exception {
        LocalDate fecha = nuevaFecha();
        long ruta = rutaProgramada("Cerropón", fecha, "MANANA");

        asignar(ruta, "AYA-104", "20000001", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CISTERNA_AUTORIZACION_VENCIDA"));
        asignar(ruta, "AYA-105", "20000001", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CISTERNA_NO_OPERATIVA"));

        asignar(ruta, "AYA-101", "20000001", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruta.cisternaPlaca").value("AYA-101"))
                .andExpect(jsonPath("$.ruta.conductorNombre").value("Carlos Quispe Vílchez"))
                .andExpect(jsonPath("$.ruta.viajesPlanificados").value(3));

        // Otra ruta en la misma fecha y turno no puede usar la misma cisterna ni el mismo conductor
        long otra = rutaProgramada("P.J. Atusparias", fecha, "MANANA");
        asignar(otra, "AYA-102", "20000001", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONDUCTOR_OCUPADO"));
        asignar(otra, "AYA-101", "20000002", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CISTERNA_OCUPADA"));
        perform(get("/api/admin/rutas/" + otra + "/sugerencias-asignacion"))
                .andExpect(jsonPath("$.cisternas[*].placa").value(Matchers.not(Matchers.hasItem("AYA-101"))))
                .andExpect(jsonPath("$.conductores[*].id").value(Matchers.not(Matchers.hasItem((int) conductor("20000001")))));

        // El admin puede corregir los viajes sugeridos
        asignar(otra, "AYA-102", "20000002", 5)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruta.viajesPlanificados").value(5));
    }

    @Test
    void conductorInactivoNoSeAsigna() throws Exception {
        long ruta = rutaProgramada("Callanca", nuevaFecha(), "TARDE");
        String dni = String.format("%08d", 22_000_000 + DIAS.incrementAndGet());
        Integer id = JsonPath.read(perform(post("/api/admin/conductores").content("""
                {"dni":"%s","nombres":"Ana","apellidos":"Baja","telefono":"9%s","password":"Inicial2026!",
                 "licencia":"Q%s"}""".formatted(dni, dni, dni)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
        perform(delete("/api/admin/conductores/" + id)).andExpect(status().isNoContent());

        asignar(ruta, "AYA-102", dni, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONDUCTOR_INACTIVO"));
    }

    // ------------------------------------------------------ listar / eliminar

    @Test
    void listarPorFechaYEliminarRutaProgramada() throws Exception {
        LocalDate fecha = nuevaFecha();
        long ruta = rutaProgramada("Valle Hermoso", fecha, "MANANA");

        perform(get("/api/admin/rutas?fecha=" + fecha))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[*].id").value(Matchers.hasItem((int) ruta)))
                .andExpect(jsonPath("$.contenido[0].totalParadas").value(Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.contenido[0].paradasAtendidas").value(0));

        perform(delete("/api/admin/rutas/" + ruta)).andExpect(status().isNoContent());
        perform(get("/api/admin/rutas/" + ruta)).andExpect(status().isNotFound());
        assertThat(contar("SELECT count(*) FROM parada WHERE ruta_id = ?", ruta)).isZero();
    }

    @Test
    void soloElAdminGestionaRutas() throws Exception {
        String tokenConductor = JsonPath.read(mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dni\":\"20000001\",\"password\":\"Conductor2026!\"}"))
                .andReturn().getResponse().getContentAsString(), "$.accessToken");
        mockMvc.perform(get("/api/admin/rutas").header("Authorization", "Bearer " + tokenConductor))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- helpers

    private static LocalDate nuevaFecha() {
        return HOY.plusDays(DIAS.incrementAndGet());
    }

    private long sectorId(String nombre) {
        return jdbc.queryForObject("SELECT id FROM sector WHERE nombre = ?", Long.class, nombre);
    }

    private long conductor(String dni) {
        return jdbc.queryForObject("SELECT id FROM persona WHERE dni = ?", Long.class, dni);
    }

    private int contar(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }

    private ResultActions programar(long sectorId, LocalDate fecha, String turno) throws Exception {
        return perform(post("/api/admin/rutas").content("""
                {"sectorId":%d,"fecha":"%s","turno":"%s","puntoLlenadoId":%d}"""
                .formatted(sectorId, fecha, turno, planta)));
    }

    private long rutaProgramada(String sector, LocalDate fecha, String turno) throws Exception {
        Integer id = JsonPath.read(programar(sectorId(sector), fecha, turno)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.ruta.id");
        return id.longValue();
    }

    private ResultActions asignar(long ruta, String placa, String dniConductor, Integer viajes) throws Exception {
        long cisterna = jdbc.queryForObject("SELECT id FROM cisterna WHERE placa = ?", Long.class, placa);
        return perform(patch("/api/admin/rutas/" + ruta + "/asignar").content("""
                {"cisternaId":%d,"conductorId":%d,"viajesPlanificados":%s}"""
                .formatted(cisterna, conductor(dniConductor), viajes)));
    }

    private ResultActions perform(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header("Authorization", "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON));
    }
}
