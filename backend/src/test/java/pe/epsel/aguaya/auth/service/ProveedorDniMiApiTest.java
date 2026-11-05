package pe.epsel.aguaya.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import pe.epsel.aguaya.auth.config.DniProperties;

class ProveedorDniMiApiTest {

    private static final String URL = "https://miapi.cloud/v1/dni/";
    private static final DniProperties PROPIEDADES = new DniProperties("miapi", URL, "token-de-prueba", 30);

    private MockRestServiceServer servidor;
    private ProveedorDniMiApi proveedor;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        proveedor = new ProveedorDniMiApi(builder.build(), PROPIEDADES);
    }

    @Test
    void devuelveNombresYApellidosCapitalizados() {
        servidor.expect(requestTo(URL + "72944911"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer token-de-prueba"))
                .andRespond(withSuccess("""
                        {"success":true,"datos":{"dni":"72944911","nombres":"NORBI ANTONI",
                         "ape_paterno":"MONTENEGRO","ape_materno":"SANCHEZ","genero":"MASCULINO",
                         "domiciliado":{"direccion":"CALLE X","distrito":"CHICLAYO"}}}""", MediaType.APPLICATION_JSON));

        ProveedorDni.DatosDni datos = proveedor.consultar("72944911");

        assertThat(datos.nombres()).isEqualTo("Norbi Antoni");
        assertThat(datos.apellidos()).isEqualTo("Montenegro Sanchez");
        servidor.verify();
    }

    @Test
    void sinApellidoMaternoUsaSoloElPaterno() {
        servidor.expect(requestTo(URL + "12345678"))
                .andRespond(withSuccess("""
                        {"success":true,"datos":{"nombres":"ANA","ape_paterno":"QUISPE","ape_materno":""}}""",
                        MediaType.APPLICATION_JSON));

        assertThat(proveedor.consultar("12345678").apellidos()).isEqualTo("Quispe");
    }

    @Test
    void documentoNoEncontradoDevuelveDniNoEncontrado() {
        servidor.expect(requestTo(URL + "12345678")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> proveedor.consultar("12345678"))
                .hasFieldOrPropertyWithValue("codigo", "DNI_NO_ENCONTRADO");
    }

    @Test
    void respuestaSinExitoDevuelveDniNoEncontrado() {
        servidor.expect(requestTo(URL + "12345678"))
                .andRespond(withSuccess("{\"success\":false}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> proveedor.consultar("12345678"))
                .hasFieldOrPropertyWithValue("codigo", "DNI_NO_ENCONTRADO");
    }

    @Test
    void limiteDeConsultasOTokenInvalidoEsErrorDelProveedor() {
        servidor.expect(requestTo(URL + "12345678")).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> proveedor.consultar("12345678"))
                .hasFieldOrPropertyWithValue("codigo", "DNI_PROVEEDOR_ERROR")
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_GATEWAY);
    }

    @Test
    void sinTokenNoArranca() {
        assertThatThrownBy(() -> new ProveedorDniMiApi(RestClient.create(),
                new DniProperties("miapi", URL, "", 30)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DNI_API_TOKEN");
    }
}
