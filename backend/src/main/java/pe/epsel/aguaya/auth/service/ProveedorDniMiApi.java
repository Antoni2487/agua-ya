package pe.epsel.aguaya.auth.service;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import pe.epsel.aguaya.auth.config.DniProperties;
import pe.epsel.aguaya.common.exception.NegocioException;

/**
 * Consulta de DNI con miapi.cloud ({@code GET {url}{dni}} con Bearer token, sin SDK).
 * Respuesta: {@code {"success":true,"datos":{"nombres":"…","ape_paterno":"…","ape_materno":"…",…}}};
 * 404 si el documento no existe, 401 si el token no sirve y 429 si se agotó el plan de consultas.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "aguaya.dni.provider", havingValue = "miapi")
public class ProveedorDniMiApi implements ProveedorDni {

    private static final Locale ES = Locale.forLanguageTag("es-PE");

    private final RestClient restClient;
    private final String url;

    @Autowired
    public ProveedorDniMiApi(RestClient.Builder builder, DniProperties properties) {
        this(builder.requestFactory(fabrica(properties.timeoutSegundos())).build(), properties);
    }

    /** Para tests: recibe el cliente ya armado (con el servidor simulado). */
    ProveedorDniMiApi(RestClient restClient, DniProperties properties) {
        if (!StringUtils.hasText(properties.token())) {
            throw new IllegalStateException("DNI_PROVIDER=miapi requiere DNI_API_TOKEN");
        }
        String token = properties.token();
        this.url = properties.url().endsWith("/") ? properties.url() : properties.url() + "/";
        this.restClient = restClient.mutate()
                .defaultHeaders(h -> {
                    h.setBearerAuth(token);
                    h.setAccept(List.of(MediaType.APPLICATION_JSON));
                })
                .build();
    }

    @Override
    public DatosDni consultar(String dni) {
        Map<?, ?> respuesta;
        try {
            respuesta = restClient.get().uri(url + dni).retrieve().body(Map.class);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                throw noEncontrado();
            }
            log.error("miapi.cloud respondió {} al consultar un DNI", e.getStatusCode().value());
            throw errorProveedor();
        } catch (RestClientException e) {
            log.error("No se pudo consultar el DNI en miapi.cloud: {}", e.getMessage());
            throw errorProveedor();
        }

        if (respuesta == null || !Boolean.TRUE.equals(respuesta.get("success"))
                || !(respuesta.get("datos") instanceof Map<?, ?> datos) || !StringUtils.hasText(texto(datos, "nombres"))) {
            throw noEncontrado();
        }
        String apellidos = Stream.of(texto(datos, "ape_paterno"), texto(datos, "ape_materno"))
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(" "));
        return new DatosDni(capitalizar(texto(datos, "nombres")), capitalizar(apellidos));
    }

    /** RENIEC entrega todo en mayúsculas: "NORBI ANTONI" → "Norbi Antoni". */
    static String capitalizar(String texto) {
        return Arrays.stream(texto.trim().toLowerCase(ES).split("\\s+"))
                .filter(p -> !p.isEmpty())
                .map(p -> p.substring(0, 1).toUpperCase(ES) + p.substring(1))
                .collect(Collectors.joining(" "));
    }

    private static String texto(Map<?, ?> datos, String clave) {
        return Objects.toString(datos.get(clave), "").trim();
    }

    private static JdkClientHttpRequestFactory fabrica(int timeoutSegundos) {
        HttpClient cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(cliente);
        fabrica.setReadTimeout(Duration.ofSeconds(timeoutSegundos));
        return fabrica;
    }

    private static NegocioException noEncontrado() {
        return new NegocioException(HttpStatus.BAD_REQUEST, "DNI_NO_ENCONTRADO",
                "No encontramos ese DNI. Revisa que esté bien escrito");
    }

    private static NegocioException errorProveedor() {
        return new NegocioException(HttpStatus.BAD_GATEWAY, "DNI_PROVEEDOR_ERROR",
                "No pudimos validar tu DNI en este momento. Intenta de nuevo en unos minutos");
    }
}
