package pe.epsel.aguaya.monitoreo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.ResultActions;

import pe.epsel.aguaya.TestcontainersConfig;
import pe.epsel.aguaya.soporte.EscenarioRutaHoy;

/**
 * Monitoreo GPS (RF04), geocerca (RF05) y estados de la ruta (regla 5) vistos por el conductor,
 * el vecino y el administrador.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class MonitoreoIntegrationTest extends EscenarioRutaHoy {

    /** ~220 m al norte de la parada: fuera del radio de 50 m. */
    private static final double LEJOS = 0.002;


    // ------------------------------------------------------------- iniciar

    @Test
    void conductorVeSuRutaDeHoyYLaInicia() throws Exception {
        Escenario e = escenario();

        mockMvc.perform(conToken(e.tokenConductor(), get("/api/conductor/rutas/hoy")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ruta.id").value((int) e.rutaId()))
                .andExpect(jsonPath("$[0].paradas.length()").value(2));

        enviarPosicion(e, -6.3, -79.4)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"));

        iniciar(e)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruta.estado").value("EN_CURSO"));
        assertThat(estadoConductor(e)).isEqualTo("EN_RUTA");

        iniciar(e)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"));
    }

    @Test
    void rutaDeOtroDiaNoSePuedeIniciar() throws Exception {
        Escenario e = escenario(HOY.plusDays(1));
        iniciar(e)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("RUTA_NO_ES_HOY"));
    }

    @Test
    void otroConductorNoVeNiOperaLaRuta() throws Exception {
        Escenario e = escenario();
        String otro = login("20000002", "Conductor2026!");
        mockMvc.perform(conToken(otro, patch("/api/conductor/rutas/" + e.rutaId() + "/iniciar")))
                .andExpect(status().isNotFound());
        mockMvc.perform(conToken(otro, patch("/api/conductor/paradas/" + e.paradas().get(0).id() + "/atender")))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------ geocerca

    @Test
    void geocercaRechazaFueraDelRadioYRegistraElIntento() throws Exception {
        Escenario e = escenario();
        Parada p = e.paradas().get(0);
        iniciar(e).andExpect(status().isOk());

        atender(e, p.id())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("SIN_POSICION_GPS"));

        enviarPosicion(e, p.latitud() + LEJOS, p.longitud()).andExpect(status().isNoContent());
        atender(e, p.id())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("FUERA_DE_GEOCERCA"))
                .andExpect(jsonPath("$.mensaje").value(Matchers.containsString("222 m")));

        // A ~10 m de la parada
        enviarPosicion(e, p.latitud() + 0.0001, p.longitud()).andExpect(status().isNoContent());
        mockMvc.perform(conToken(e.tokenConductor(), patch("/api/conductor/paradas/" + p.id() + "/atender"))
                        .content("{\"volumenEntregadoLitros\":1200}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ATENDIDA"));
        assertThat(jdbc.queryForObject("SELECT verificada_gps FROM parada WHERE id = ?", Boolean.class, p.id())).isTrue();
        assertThat(jdbc.queryForObject("SELECT volumen_entregado_litros FROM parada WHERE id = ?", Integer.class, p.id()))
                .isEqualTo(1200);

        atender(e, p.id())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PARADA_NO_PENDIENTE"));

        // El admin ve el recorrido y los dos intentos rechazados
        mockMvc.perform(conToken(login("10000001", "Admin2026!"), get("/api/admin/rutas/" + e.rutaId() + "/recorrido")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posiciones.length()").value(2))
                .andExpect(jsonPath("$.intentosFallidos.length()").value(2))
                .andExpect(jsonPath("$.intentosFallidos[0].distanciaMetros").doesNotExist())
                .andExpect(jsonPath("$.intentosFallidos[1].distanciaMetros").value(Matchers.closeTo(222.0, 2.0)));
        assertThat(jdbc.queryForObject("SELECT lat_actual FROM conductor WHERE persona_id = ?", BigDecimal.class,
                e.conductorId()).doubleValue()).isCloseTo(p.latitud() + 0.0001, org.assertj.core.data.Offset.offset(1e-6));
    }

    @Test
    void posicionDeHaceMasDeDosMinutosNoValidaLaGeocerca() throws Exception {
        Escenario e = escenario();
        Parada p = e.paradas().get(0);
        iniciar(e).andExpect(status().isOk());
        enviarPosicion(e, p.latitud(), p.longitud()).andExpect(status().isNoContent());

        // El conductor dejó de enviar GPS hace 3 minutos junto a la parada
        jdbc.update("UPDATE conductor SET ubicacion_actualizada_en = now() - interval '3 minutes' WHERE persona_id = ?",
                e.conductorId());
        atender(e, p.id())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("POSICION_DESACTUALIZADA"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM intento_geocerca WHERE parada_id = ?", Integer.class, p.id()))
                .isEqualTo(1);

        // Con una posición nueva se puede atender
        enviarPosicion(e, p.latitud(), p.longitud()).andExpect(status().isNoContent());
        atender(e, p.id()).andExpect(status().isOk());
    }

    // ------------------------------------------------------------ finalizar

    @Test
    void finalizarExigeTodasLasParadasEnEstadoFinal() throws Exception {
        Escenario e = escenario();
        Parada p1 = e.paradas().get(0);
        Parada p2 = e.paradas().get(1);
        iniciar(e).andExpect(status().isOk());

        finalizar(e)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("RUTA_CON_PARADAS_PENDIENTES"));

        enviarPosicion(e, p1.latitud(), p1.longitud()).andExpect(status().isNoContent());
        atender(e, p1.id()).andExpect(status().isOk());
        mockMvc.perform(conToken(e.tokenConductor(), patch("/api/conductor/paradas/" + p2.id() + "/no-atendida")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("NO_ATENDIDA"));

        finalizar(e)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruta.estado").value("COMPLETADA"))
                .andExpect(jsonPath("$.ruta.paradasAtendidas").value(1));
        assertThat(estadoConductor(e)).isEqualTo("DISPONIBLE");

        enviarPosicion(e, p1.latitud(), p1.longitud())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"));
    }

    // -------------------------------------------------------------- vecino

    @Test
    void vecinoDelSectorVeLaCisternaSoloMientrasLaRutaEstaEnCurso() throws Exception {
        Escenario e = escenario();
        String tokenVecino = vecinoDelSector(e);
        String tokenVecinoOtroSector = login("30000004", "Vecino2026!");

        posicionComoVecino(tokenVecino, e)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("RUTA_NO_EN_CURSO"));

        iniciar(e).andExpect(status().isOk());
        Parada p = e.paradas().get(0);
        enviarPosicion(e, p.latitud(), p.longitud()).andExpect(status().isNoContent());

        posicionComoVecino(tokenVecino, e)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rutaId").value((int) e.rutaId()))
                .andExpect(jsonPath("$.latitud").value(Matchers.closeTo(p.latitud(), 1e-6)));
        posicionComoVecino(tokenVecinoOtroSector, e).andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------- helpers

    private ResultActions atender(Escenario e, long paradaId) throws Exception {
        return mockMvc.perform(conToken(e.tokenConductor(), patch("/api/conductor/paradas/" + paradaId + "/atender")));
    }

    private ResultActions finalizar(Escenario e) throws Exception {
        return mockMvc.perform(conToken(e.tokenConductor(), patch("/api/conductor/rutas/" + e.rutaId() + "/finalizar")));
    }

    private ResultActions posicionComoVecino(String token, Escenario e) throws Exception {
        return mockMvc.perform(conToken(token, get("/api/vecino/rutas/" + e.rutaId() + "/posicion")));
    }

    private String estadoConductor(Escenario e) {
        return jdbc.queryForObject("SELECT estado_disponibilidad FROM conductor WHERE persona_id = ?", String.class,
                e.conductorId());
    }

}
