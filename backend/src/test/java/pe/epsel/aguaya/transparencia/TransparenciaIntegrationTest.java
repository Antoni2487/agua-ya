package pe.epsel.aguaya.transparencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import com.jayway.jsonpath.JsonPath;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import pe.epsel.aguaya.TestcontainersConfig;
import pe.epsel.aguaya.soporte.EscenarioRutaHoy;

/**
 * Panel público de transparencia (RF09): sin login, agregado y sin datos personales (regla 9).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class TransparenciaIntegrationTest extends EscenarioRutaHoy {

    @Test
    void indicadoresDeUnaRutaCerrada() throws Exception {
        Escenario e = escenario();
        String vecino = vecinoDelSector(e);
        Parada atendida = e.paradas().get(0);
        Parada noAtendida = e.paradas().get(1);
        String c = e.tokenConductor();

        iniciar(e).andExpect(status().isOk());
        String viaje = mockMvc.perform(conToken(c, post("/api/conductor/rutas/" + e.rutaId() + "/viajes"))
                        .content("{\"volumenCargadoLitros\":5000}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        enviarPosicion(e, atendida.latitud(), atendida.longitud()).andExpect(status().isNoContent());
        mockMvc.perform(conToken(c, patch("/api/conductor/paradas/" + atendida.id() + "/atender")))
                .andExpect(status().isOk());
        mockMvc.perform(conToken(c, patch("/api/conductor/paradas/" + noAtendida.id() + "/no-atendida")))
                .andExpect(status().isOk());
        mockMvc.perform(conToken(c, patch("/api/conductor/viajes/" + JsonPath.read(viaje, "$.id") + "/finalizar")))
                .andExpect(status().isOk());
        mockMvc.perform(conToken(c, patch("/api/conductor/rutas/" + e.rutaId() + "/finalizar")))
                .andExpect(status().isOk());
        mockMvc.perform(conToken(vecino, post("/api/vecino/calificaciones"))
                        .content("{\"paradaId\":%d,\"estrellas\":4}".formatted(atendida.id())))
                .andExpect(status().isCreated());

        String sectores = publico("/sectores?desde=%s&hasta=%s".formatted(HOY, HOY));
        List<Map<String, Object>> suyo = JsonPath.read(sectores, "$[?(@.sectorId == %d)]".formatted(e.sectorId()));
        assertThat(suyo).singleElement().satisfies(s -> {
            assertThat(s).containsEntry("paradasAtendidas", 1)
                    .containsEntry("cumplimientoPorcentaje", 50.0)
                    .containsEntry("calificacionPromedio", 4.0)
                    .containsEntry("ultimaAtencion", HOY.toString());
            assertThat(s.get("proximaAtencion")).isNull();
        });

        String resumen = publico("/resumen?desde=%s&hasta=%s".formatted(HOY, HOY));
        assertThat((int) JsonPath.read(resumen, "$.rutas.completadas")).isGreaterThanOrEqualTo(1);
        assertThat(((Number) JsonPath.read(resumen, "$.litrosDistribuidos")).longValue()).isGreaterThanOrEqualTo(5000);
        assertThat((int) JsonPath.read(resumen, "$.paradas.atendidas")).isGreaterThanOrEqualTo(1);
        assertThat((int) JsonPath.read(resumen, "$.calificacion.total")).isGreaterThanOrEqualTo(1);
        assertThat((int) JsonPath.read(resumen, "$.sectoresActivos")).isGreaterThanOrEqualTo(5);
        assertThat((int) JsonPath.read(resumen, "$.flota.total")).isGreaterThanOrEqualTo(5);

        // Regla 9: ni nombres, ni documentos, ni teléfonos, ni coordenadas de viviendas o paradas.
        for (String body : List.of(sectores, resumen, publico("/cronograma?desde=" + HOY))) {
            assertThat(body).doesNotContain("Vecina", "Conductor", "dni", "telefono", "\"latitud\"", "\"longitud\"");
        }
    }

    @Test
    void cronogramaYProximaAtencionDeUnaRutaFutura() throws Exception {
        Escenario e = escenario(HOY.plusDays(2));

        mockMvc.perform(get("/api/public/transparencia/cronograma"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.rutaId == %d)].estado".formatted(e.rutaId())).value("PROGRAMADA"))
                .andExpect(jsonPath("$[?(@.rutaId == %d)].fecha".formatted(e.rutaId())).value(HOY.plusDays(2).toString()))
                .andExpect(jsonPath("$[?(@.rutaId == %d)].totalParadas".formatted(e.rutaId())).value(2))
                .andExpect(jsonPath("$[?(@.rutaId == %d)].cisternaPlaca".formatted(e.rutaId()),
                        Matchers.contains(Matchers.startsWith("MON-"))));
        mockMvc.perform(get("/api/public/transparencia/cronograma?distrito=monsefú"))
                .andExpect(jsonPath("$[*].rutaId", Matchers.hasItem((int) e.rutaId())));
        mockMvc.perform(get("/api/public/transparencia/cronograma?distrito=Chiclayo"))
                .andExpect(jsonPath("$[*].rutaId", Matchers.not(Matchers.hasItem((int) e.rutaId()))));
        mockMvc.perform(get("/api/public/transparencia/cronograma?desde=" + HOY.plusDays(3)))
                .andExpect(jsonPath("$[*].rutaId", Matchers.not(Matchers.hasItem((int) e.rutaId()))));

        mockMvc.perform(get("/api/public/transparencia/sectores?distrito=Monsefú"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.sectorId == %d)].proximaAtencion.fecha".formatted(e.sectorId()))
                        .value(HOY.plusDays(2).toString()))
                .andExpect(jsonPath("$[?(@.sectorId == %d)].cumplimientoPorcentaje".formatted(e.sectorId()))
                        .value(Matchers.contains(Matchers.nullValue())))
                .andExpect(jsonPath("$[*].distrito", Matchers.everyItem(Matchers.is("Monsefú"))));
    }

    @Test
    void esPublicoYValidaElPeriodo() throws Exception {
        mockMvc.perform(get("/api/public/transparencia/resumen")).andExpect(status().isOk())
                .andExpect(jsonPath("$.hasta").value(HOY.toString()))
                .andExpect(jsonPath("$.desde").value(HOY.minusDays(29).toString()));
        mockMvc.perform(get("/api/public/transparencia/resumen?desde=%s&hasta=%s".formatted(HOY, HOY.minusDays(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PERIODO_INVALIDO"));
        mockMvc.perform(get("/api/public/transparencia/cronograma?desde=%s&hasta=%s".formatted(HOY, HOY.plusDays(40))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PERIODO_INVALIDO"));
        mockMvc.perform(get("/api/public/transparencia/sectores?desde=no-es-fecha")).andExpect(status().isBadRequest());
    }

    private String publico(String ruta) throws Exception {
        return mockMvc.perform(get("/api/public/transparencia" + ruta))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }
}
