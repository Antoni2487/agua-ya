package pe.epsel.aguaya.monitoreo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Base de los tests de monitoreo: arma una ruta de HOY en un sector propio (lejos de los datos de
 * prueba), con 2 puntos de entrega, una cisterna y un conductor nuevos.
 */
abstract class EscenarioMonitoreo {

    static final AtomicInteger SECUENCIA = new AtomicInteger(1);
    static final LocalDate HOY = LocalDate.now(ZoneId.of("America/Lima"));

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    /** Ruta asignada y lista para que el conductor la inicie. */
    record Escenario(long rutaId, long sectorId, long conductorId, String tokenConductor,
                     List<Parada> paradas) {
    }

    record Parada(long id, double latitud, double longitud) {
    }

    Escenario escenario() throws Exception {
        return escenario(HOY);
    }

    Escenario escenario(LocalDate fecha) throws Exception {
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

    String login(String dni, String password) throws Exception {
        return JsonPath.read(mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dni\":\"%s\",\"password\":\"%s\"}".formatted(dni, password)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.accessToken");
    }

    ResultActions iniciar(Escenario e) throws Exception {
        return mockMvc.perform(conToken(e.tokenConductor(), patch("/api/conductor/rutas/" + e.rutaId() + "/iniciar")));
    }

    ResultActions enviarPosicion(Escenario e, double lat, double lng) throws Exception {
        return mockMvc.perform(conToken(e.tokenConductor(), post("/api/conductor/rutas/" + e.rutaId() + "/posicion"))
                .content("{\"latitud\":%s,\"longitud\":%s}".formatted(lat, lng)));
    }

    static MockHttpServletRequestBuilder conToken(String token, MockHttpServletRequestBuilder request) {
        return request.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }

    private long idDe(MockHttpServletRequestBuilder request, String path) throws Exception {
        String body = mockMvc.perform(request).andExpect(status().isCreated()).andReturn().getResponse()
                .getContentAsString();
        return ((Number) JsonPath.read(body, path)).longValue();
    }
}
