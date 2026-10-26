package pe.epsel.aguaya;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
 * CRUD de catálogos del administrador. Usa las cuentas y datos de V3__datos_referenciales.sql.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class CatalogosIntegrationTest {

    private static final AtomicInteger SECUENCIA = new AtomicInteger(100);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private String tokenAdmin;

    @BeforeEach
    void loginAdmin() throws Exception {
        tokenAdmin = login("10000001", "Admin2026!");
    }

    // ---------------------------------------------------------------- sectores

    @Test
    void listarSectoresPaginadoYFiltrado() throws Exception {
        perform(get("/api/admin/sectores?size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(Matchers.greaterThanOrEqualTo(5)));

        perform(get("/api/admin/sectores?distrito=Monsefú"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[*].distrito").value(Matchers.everyItem(Matchers.is("Monsefú"))))
                .andExpect(jsonPath("$.contenido.length()").value(Matchers.greaterThanOrEqualTo(2)));
    }

    @Test
    void crearActualizarYDesactivarSector() throws Exception {
        String nombre = "Sector Prueba " + SECUENCIA.incrementAndGet();
        String body = perform(post("/api/admin/sectores").content(sector(nombre, 3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value(nombre))
                .andExpect(jsonPath("$.dotacionLitros").value(80))
                .andExpect(jsonPath("$.activo").value(true))
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(body, "$.id");

        perform(put("/api/admin/sectores/" + id).content(sector(nombre, 5)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxViajesDiarios").value(5));

        perform(delete("/api/admin/sectores/" + id)).andExpect(status().isNoContent());
        perform(get("/api/admin/sectores/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    void sectorDuplicadoDevuelve409() throws Exception {
        perform(post("/api/admin/sectores").content(sector("Cerropón", 3).replace("\"Chiclayo\"", "\"chiclayo\"")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("SECTOR_DUPLICADO"));
    }

    @Test
    void sectorInvalidoDevuelve400() throws Exception {
        perform(post("/api/admin/sectores").content(sector("Sin viajes " + SECUENCIA.incrementAndGet(), 0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles[0].campo").value("maxViajesDiarios"));
    }

    @Test
    void ordenarPorCampoInexistenteDevuelve400() throws Exception {
        perform(get("/api/admin/sectores?sort=noExiste"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("ORDEN_INVALIDO"));
    }

    @Test
    void sectorInexistenteDevuelve404() throws Exception {
        perform(get("/api/admin/sectores/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }

    // ----------------------------------------------------------- instituciones

    @Test
    void crearInstitucionYFiltrarPorSector() throws Exception {
        Long sectorId = jdbc.queryForObject("SELECT id FROM sector WHERE nombre = 'Callanca'", Long.class);
        perform(post("/api/admin/instituciones").content("""
                {"nombre":"Compañía de Bomberos Prueba","tipo":"BOMBEROS","sectorId":%d,
                 "latitud":-6.8581,"longitud":-79.8519}""".formatted(sectorId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sectorNombre").value("Callanca"))
                .andExpect(jsonPath("$.tipo").value("BOMBEROS"));

        perform(get("/api/admin/instituciones?sectorId=" + sectorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[*].sectorId").value(Matchers.everyItem(Matchers.is(sectorId.intValue()))))
                .andExpect(jsonPath("$.contenido.length()").value(Matchers.greaterThanOrEqualTo(3)));
    }

    @Test
    void institucionConSectorInexistenteDevuelve404() throws Exception {
        perform(post("/api/admin/instituciones").content("""
                {"nombre":"Posta X","tipo":"SALUD","sectorId":999999,"latitud":-6.8,"longitud":-79.8}"""))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------- puntos de llenado

    @Test
    void crearYDesactivarPuntoDeLlenado() throws Exception {
        String body = perform(post("/api/admin/puntos-llenado").content("""
                {"nombre":"Pozo Prueba","tipo":"POZO","direccion":"Monsefú","latitud":-6.87,"longitud":-79.87}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("POZO"))
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(body, "$.id");

        perform(delete("/api/admin/puntos-llenado/" + id)).andExpect(status().isNoContent());
        perform(get("/api/admin/puntos-llenado?activo=false"))
                .andExpect(jsonPath("$.contenido[*].id").value(Matchers.hasItem(id)));
    }

    // --------------------------------------------------------------- cisternas

    @Test
    void cisternasMuestranSiLaAutorizacionEstaVigente() throws Exception {
        perform(get("/api/admin/cisternas?size=50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[?(@.placa=='AYA-101')].autorizacionVigente").value(true))
                .andExpect(jsonPath("$.contenido[?(@.placa=='AYA-104')].autorizacionVigente").value(false));

        perform(get("/api/admin/cisternas?estado=MANTENIMIENTO"))
                .andExpect(jsonPath("$.contenido[*].placa").value(Matchers.hasItem("AYA-105")));
    }

    @Test
    void crearCisternaNormalizaLaPlacaYRechazaDuplicadas() throws Exception {
        String placa = "tst-" + String.format("%03d", SECUENCIA.incrementAndGet() % 1000);
        perform(post("/api/admin/cisternas").content(cisterna(placa)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.placa").value(placa.toUpperCase()))
                .andExpect(jsonPath("$.estadoVehiculo").value("OPERATIVA"))
                .andExpect(jsonPath("$.autorizacionVigente").value(true));

        perform(post("/api/admin/cisternas").content(cisterna(placa.toUpperCase())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PLACA_DUPLICADA"));
    }

    @Test
    void placaConFormatoInvalidoDevuelve400() throws Exception {
        perform(post("/api/admin/cisternas").content(cisterna("123456")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles[0].campo").value("placa"));
    }

    @Test
    void eliminarCisternaSoloSiNoTieneRutas() throws Exception {
        Integer sinRutas = crearCisterna();
        perform(delete("/api/admin/cisternas/" + sinRutas)).andExpect(status().isNoContent());
        perform(get("/api/admin/cisternas/" + sinRutas)).andExpect(status().isNotFound());

        Integer conRuta = crearCisterna();
        jdbc.update("INSERT INTO ruta (fecha_programada, turno, sector_id, cisterna_id, administrador_id) "
                + "VALUES (CURRENT_DATE, 'MANANA', (SELECT min(id) FROM sector), ?, "
                + "(SELECT min(persona_id) FROM administrador))", conRuta);
        perform(delete("/api/admin/cisternas/" + conRuta))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CISTERNA_CON_RUTAS"));
    }

    // -------------------------------------------------------------- conductores

    @Test
    void conductorCreadoPorElAdminPuedeIngresar() throws Exception {
        String dni = String.format("%08d", 21_000_000 + SECUENCIA.incrementAndGet());
        perform(post("/api/admin/conductores").content(conductor(dni)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dni").value(dni))
                .andExpect(jsonPath("$.licencia").value("Q" + dni))
                .andExpect(jsonPath("$.estadoDisponibilidad").value("DISPONIBLE"))
                .andExpect(jsonPath("$.password").doesNotExist());

        loginOk(dni, "Inicial2026!");
    }

    @Test
    void conductorConDniExistenteDevuelve409() throws Exception {
        perform(post("/api/admin/conductores").content(conductor("20000001")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("DNI_REGISTRADO"));
    }

    @Test
    void cambioDePasswordYDesactivacionDeConductor() throws Exception {
        String dni = String.format("%08d", 21_000_000 + SECUENCIA.incrementAndGet());
        Integer id = JsonPath.read(perform(post("/api/admin/conductores").content(conductor(dni)))
                .andReturn().getResponse().getContentAsString(), "$.id");

        perform(put("/api/admin/conductores/" + id).content(actualizacionConductor(dni, "\"EN_RUTA\"", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("ESTADO_NO_PERMITIDO"));

        perform(put("/api/admin/conductores/" + id).content(actualizacionConductor(dni, null, "\"Nueva2026!\"")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turno").value("TARDE"));
        loginFalla(dni, "Inicial2026!");
        loginOk(dni, "Nueva2026!");

        perform(delete("/api/admin/conductores/" + id)).andExpect(status().isNoContent());
        perform(get("/api/admin/conductores/" + id))
                .andExpect(jsonPath("$.activo").value(false))
                .andExpect(jsonPath("$.estadoDisponibilidad").value("INACTIVO"));
        loginFalla(dni, "Nueva2026!");
    }

    // ------------------------------------------------------------------ roles

    @Test
    void soloElAdminGestionaCatalogos() throws Exception {
        String tokenVecino = login("30000001", "Vecino2026!");
        String tokenConductor = login("20000001", "Conductor2026!");
        for (String token : new String[] {tokenVecino, tokenConductor}) {
            mockMvc.perform(get("/api/admin/cisternas").header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/api/admin/sectores")).andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------- helpers

    private ResultActions perform(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header("Authorization", "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON));
    }

    private String login(String dni, String password) throws Exception {
        return JsonPath.read(loginOk(dni, password).andReturn().getResponse().getContentAsString(), "$.accessToken");
    }

    private ResultActions loginOk(String dni, String password) throws Exception {
        return postLogin(dni, password).andExpect(status().isOk());
    }

    private void loginFalla(String dni, String password) throws Exception {
        postLogin(dni, password).andExpect(status().isUnauthorized());
    }

    private ResultActions postLogin(String dni, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"dni\":\"%s\",\"password\":\"%s\"}".formatted(dni, password)));
    }

    private Integer crearCisterna() throws Exception {
        String placa = "DEL-" + String.format("%03d", SECUENCIA.incrementAndGet() % 1000);
        return JsonPath.read(perform(post("/api/admin/cisternas").content(cisterna(placa)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
    }

    private static String sector(String nombre, int maxViajes) {
        return """
                {"nombre":"%s","distrito":"Chiclayo","modalidad":"PERMANENTE","esZonaAlta":true,
                 "unidadesUso":120,"frecuenciaDias":1,"maxViajesDiarios":%d,
                 "latCentro":-6.79,"lngCentro":-79.83}""".formatted(nombre, maxViajes);
    }

    private static String cisterna(String placa) {
        return """
                {"placa":"%s","capacidadLitros":10000,"nroAutorizacionSanitaria":"REF-TEST",
                 "venceAutorizacion":"2099-12-31"}""".formatted(placa);
    }

    private static String conductor(String dni) {
        return """
                {"dni":"%s","nombres":"Luis","apellidos":"Prueba","telefono":"9%s",
                 "password":"Inicial2026!","licencia":"q%s","turno":"MANANA"}""".formatted(dni, dni.substring(0, 8), dni);
    }

    private static String actualizacionConductor(String dni, String estado, String password) {
        return """
                {"nombres":"Luis","apellidos":"Prueba","telefono":"9%s","licencia":"Q%s","turno":"TARDE",
                 "estadoDisponibilidad":%s,"password":%s}""".formatted(dni, dni, estado, password);
    }
}
