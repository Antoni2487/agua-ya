package pe.epsel.aguaya.persona;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import pe.epsel.aguaya.TestcontainersConfig;
import pe.epsel.aguaya.soporte.EscenarioRutaHoy;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class VecinoPerfilIntegrationTest extends EscenarioRutaHoy {

    @Test
    void elVecinoVeSusDatosYSuSector() throws Exception {
        mockMvc.perform(conToken(login("30000001", "Vecino2026!"), get("/api/vecino/me")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dni").value("30000001"))
                .andExpect(jsonPath("$.nombres").isNotEmpty())
                .andExpect(jsonPath("$.latitud").isNumber())
                .andExpect(jsonPath("$.sector.nombre").isNotEmpty())
                .andExpect(jsonPath("$.sector.tipoEntrega").isNotEmpty())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void soloElRolVecino() throws Exception {
        mockMvc.perform(conToken(login("20000001", "Conductor2026!"), get("/api/vecino/me")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/vecino/me")).andExpect(status().isUnauthorized());
    }
}
