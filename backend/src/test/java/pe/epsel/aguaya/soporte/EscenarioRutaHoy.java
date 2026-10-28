package pe.epsel.aguaya.soporte;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import pe.epsel.aguaya.persona.entity.Vecino;
import pe.epsel.aguaya.persona.repository.VecinoRepository;
import pe.epsel.aguaya.sector.repository.SectorRepository;

/**
 * Base de los tests de operación de rutas (monitoreo, incidencias): arma una ruta de HOY en un sector propio (lejos de los datos de
 * prueba), con 2 puntos de entrega, una cisterna y un conductor nuevos.
 */
public abstract class EscenarioRutaHoy {

    protected static final AtomicInteger SECUENCIA = new AtomicInteger(1);
    protected static final LocalDate HOY = LocalDate.now(ZoneId.of("America/Lima"));

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    private VecinoRepository vecinoRepository;

    @Autowired
    private SectorRepository sectorRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /** Ruta asignada y lista para que el conductor la inicie. */
    public record Escenario(long rutaId, long sectorId, long conductorId, String tokenConductor,
                     List<Parada> paradas) {
    }

    public record Parada(long id, double latitud, double longitud) {
    }

    protected Escenario escenario() throws Exception {
        return escenario(HOY);
    }

    protected Escenario escenario(LocalDate fecha) throws Exception {
        int n = SECUENCIA.incrementAndGet();
        String admin = login("10000001", "Admin2026!");
        double lat = -6.300 - n * 0.05;
        double lng = -79.400;

        long sectorId = idDe(conToken(admin, post("/api/admin/sectores")).content("""
                {"nombre":"Monitoreo %d","distrito":"Monsefú","modalidad":"CONTINGENCIA","esZonaAlta":true,
                 "unidadesUso":100,"frecuenciaDias":1,"maxViajesDiarios":3,"latCentro":%s,"lngCentro":%s,
                 "tipoEntrega":"PUNTO_ENTREGA"}""".formatted(n, lat, lng)), "$.id");
        for (double dLat : new double[] {0, 0.002}) {
            idDe(conToken(admin, post("/api/admin/puntos-entrega")).content("""
                    {"sectorId":%d,"nombre":"Punto %s","latitud":%s,"longitud":%s}"""
                    .formatted(sectorId, dLat, lat + dLat, lng)), "$.id");
        }
        long cisternaId = idDe(conToken(admin, post("/api/admin/cisternas")).content("""
                {"placa":"MON-%03d","capacidadLitros":10000,"venceAutorizacion":"2099-12-31"}"""
                .formatted(n % 1000)), "$.id");
        String dni = String.format("%08d", 23_000_000 + n);
        long conductorId = idDe(conToken(admin, post("/api/admin/conductores")).content("""
                {"dni":"%s","nombres":"Conductor","apellidos":"Monitoreo %d","telefono":"9%s",
                 "password":"Conductor2026!","licencia":"Q%s","turno":"MANANA"}"""
                .formatted(dni, n, dni, dni)), "$.id");
        long planta = jdbc.queryForObject("SELECT min(id) FROM punto_llenado WHERE tipo = 'PLANTA'", Long.class);

        String ruta = mockMvc.perform(conToken(admin, post("/api/admin/rutas")).content("""
                {"sectorId":%d,"fecha":"%s","turno":"MANANA","puntoLlenadoId":%d}"""
                .formatted(sectorId, fecha, planta))).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long rutaId = ((Number) JsonPath.read(ruta, "$.ruta.id")).longValue();
        mockMvc.perform(conToken(admin, patch("/api/admin/rutas/" + rutaId + "/asignar")).content("""
                {"cisternaId":%d,"conductorId":%d}""".formatted(cisternaId, conductorId)))
                .andExpect(status().isOk());

        List<Number> ids = JsonPath.read(ruta, "$.paradas[*].id");
        List<Number> lats = JsonPath.read(ruta, "$.paradas[*].latitud");
        List<Number> lngs = JsonPath.read(ruta, "$.paradas[*].longitud");
        List<Parada> paradas = new java.util.ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            paradas.add(new Parada(ids.get(i).longValue(), lats.get(i).doubleValue(), lngs.get(i).doubleValue()));
        }
        return new Escenario(rutaId, sectorId, conductorId, login(dni, "Conductor2026!"), paradas);
    }

    protected String login(String dni, String password) throws Exception {
        return JsonPath.read(mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dni\":\"%s\",\"password\":\"%s\"}".formatted(dni, password)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.accessToken");
    }

    protected ResultActions iniciar(Escenario e) throws Exception {
        return mockMvc.perform(conToken(e.tokenConductor(), patch("/api/conductor/rutas/" + e.rutaId() + "/iniciar")));
    }

    protected ResultActions enviarPosicion(Escenario e, double lat, double lng) throws Exception {
        return mockMvc.perform(conToken(e.tokenConductor(), post("/api/conductor/rutas/" + e.rutaId() + "/posicion"))
                .content("{\"latitud\":%s,\"longitud\":%s}".formatted(lat, lng)));
    }

    protected static MockHttpServletRequestBuilder conToken(String token, MockHttpServletRequestBuilder request) {
        return request.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }

    /** Crea un vecino con su vivienda junto a la primera parada del escenario y devuelve su token. */
    protected String vecinoDelSector(Escenario e) throws Exception {
        int n = SECUENCIA.incrementAndGet();
        String dni = String.format("%08d", 33_000_000 + n);
        Vecino vecino = new Vecino(dni, "Vecina", "Prueba", "+519" + String.format("%08d", 33_000_000 + n),
                passwordEncoder.encode("Vecino2026!"));
        Parada p = e.paradas().get(0);
        vecino.setLatitud(BigDecimal.valueOf(p.latitud()));
        vecino.setLongitud(BigDecimal.valueOf(p.longitud()));
        vecino.setSector(sectorRepository.getReferenceById(e.sectorId()));
        vecinoRepository.save(vecino);
        return login(dni, "Vecino2026!");
    }

    private long idDe(MockHttpServletRequestBuilder request, String path) throws Exception {
        String body = mockMvc.perform(request).andExpect(status().isCreated()).andReturn().getResponse()
                .getContentAsString();
        return ((Number) JsonPath.read(body, path)).longValue();
    }
}
