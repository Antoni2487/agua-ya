package pe.epsel.aguaya.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.atomic.AtomicInteger;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import pe.epsel.aguaya.TestcontainersConfig;
import pe.epsel.aguaya.auth.service.ProveedorDniLocal;
import pe.epsel.aguaya.persona.entity.Administrador;
import pe.epsel.aguaya.persona.entity.Conductor;
import pe.epsel.aguaya.persona.entity.Vecino;
import pe.epsel.aguaya.persona.repository.PersonaRepository;
import pe.epsel.aguaya.persona.repository.VecinoRepository;
import pe.epsel.aguaya.sector.entity.Sector;
import pe.epsel.aguaya.sector.repository.SectorRepository;

@SpringBootTest(properties = "aguaya.otp.exponer-codigo=true")
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class AuthIntegrationTest {

    /** Cada test usa DNI y teléfono propios para no interferir con los demás. */
    private static final AtomicInteger SECUENCIA = new AtomicInteger(1000);
    private static final String PASSWORD = "Clave1234";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SectorRepository sectorRepository;

    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private VecinoRepository vecinoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /** Los sectores vienen de V3__datos_referenciales.sql. */
    private Sector sector(String nombre) {
        return sectorRepository.findAll().stream().filter(s -> s.getNombre().equals(nombre))
                .findFirst().orElseThrow();
    }

    // ---------------------------------------------------------------- registro

    @Test
    void registroDeVecinoAsignaSectorMasCercanoYAbreSesion() throws Exception {
        String telefono = telefonoNuevo();
        String dni = dniNuevo();

        // Pin junto al centro de Valle Hermoso (Monsefú)
        registrar(dni, telefono, solicitarOtp(telefono), "-6.892800", "-79.885200")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tipoToken").value("Bearer"))
                .andExpect(jsonPath("$.usuario.rol").value("VECINO"));

        Vecino vecino = vecinoRepository.findByTelefono("+51" + telefono).orElseThrow();
        assertThat(vecino.getDni()).isEqualTo(dni);
        assertThat(vecino.isActivo()).isTrue();
        // Nombres desde el proveedor de DNI (local en tests); el vecino no tiene contraseña
        assertThat(vecino.getNombres()).isEqualTo("Vecino");
        assertThat(vecino.getApellidos()).isEqualTo("De Prueba " + dni.substring(4));
        assertThat(vecino.getPasswordHash()).isNull();
        assertThat(vecino.getSector().getId()).isEqualTo(sector("Valle Hermoso").getId());
    }

    @Test
    void vecinoRegistradoIngresaConDniYCodigoSms() throws Exception {
        String dni = dniNuevo();
        String telefono = telefonoNuevo();
        registrar(dni, telefono, solicitarOtp(telefono), "-6.765000", "-79.850000")
                .andExpect(status().isCreated());

        String body = postJson("/api/auth/login/codigo/solicitar", """
                {"dni":"%s"}""".formatted(dni))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefonoEnmascarado").value("*** *** " + telefono.substring(6)))
                .andReturn().getResponse().getContentAsString();

        postJson("/api/auth/login/codigo", """
                {"dni":"%s","codigo":"%s"}""".formatted(dni, JsonPath.read(body, "$.codigo")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.usuario.rol").value("VECINO"));
    }

    @Test
    void vecinoSinContrasenaNoIngresaPorLoginConContrasena() throws Exception {
        String dni = dniNuevo();
        String telefono = telefonoNuevo();
        registrar(dni, telefono, solicitarOtp(telefono), "-6.765000", "-79.850000")
                .andExpect(status().isCreated());

        login(dni, PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
    }

    @Test
    void codigoDeIngresoIncorrectoDevuelve400() throws Exception {
        String dni = dniNuevo();
        String telefono = telefonoNuevo();
        registrar(dni, telefono, solicitarOtp(telefono), "-6.765000", "-79.850000")
                .andExpect(status().isCreated());
        String codigo = JsonPath.read(postJson("/api/auth/login/codigo/solicitar", """
                {"dni":"%s"}""".formatted(dni)).andReturn().getResponse().getContentAsString(), "$.codigo");

        postJson("/api/auth/login/codigo", """
                {"dni":"%s","codigo":"%s"}""".formatted(dni, codigo.equals("000000") ? "111111" : "000000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("OTP_INCORRECTO"));
    }

    @Test
    void codigoDeIngresoParaDniSinCuentaDevuelve404() throws Exception {
        postJson("/api/auth/login/codigo/solicitar", """
                {"dni":"%s"}""".formatted(dniNuevo()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("DNI_NO_REGISTRADO"));
    }

    @Test
    void administradorNoIngresaConCodigoSms() throws Exception {
        postJson("/api/auth/login/codigo/solicitar", """
                {"dni":"%s"}""".formatted(crearAdmin()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("INGRESO_CON_PASSWORD"));
    }

    @Test
    void vecinoInactivoNoRecibeCodigoDeIngreso() throws Exception {
        String dni = dniNuevo();
        String telefono = telefonoNuevo();
        registrar(dni, telefono, solicitarOtp(telefono), "-6.765000", "-79.850000")
                .andExpect(status().isCreated());
        Vecino vecino = vecinoRepository.findByTelefono("+51" + telefono).orElseThrow();
        vecino.setActivo(false);
        vecinoRepository.save(vecino);

        postJson("/api/auth/login/codigo/solicitar", """
                {"dni":"%s"}""".formatted(dni))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("CUENTA_INACTIVA"));
    }

    @Test
    void registroConDniDuplicadoDevuelve409() throws Exception {
        String dni = dniNuevo();
        String telefono = telefonoNuevo();
        registrar(dni, telefono, solicitarOtp(telefono), "-6.765000", "-79.850000")
                .andExpect(status().isCreated());

        String otroTelefono = telefonoNuevo();
        registrar(dni, otroTelefono, solicitarOtp(otroTelefono), "-6.765000", "-79.850000")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("DNI_REGISTRADO"));
    }

    @Test
    void solicitarOtpConTelefonoYaRegistradoDevuelve409() throws Exception {
        String telefono = telefonoNuevo();
        registrar(dniNuevo(), telefono, solicitarOtp(telefono), "-6.765000", "-79.850000")
                .andExpect(status().isCreated());

        postJson("/api/auth/otp/solicitar", """
                {"telefono":"%s"}""".formatted(telefono))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TELEFONO_REGISTRADO"));
    }

    @Test
    void dniQueNoExisteEnReniecNoSeRegistra() throws Exception {
        String telefono = telefonoNuevo();
        registrar(ProveedorDniLocal.DNI_INEXISTENTE, telefono, solicitarOtp(telefono), "-6.765000", "-79.850000")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DNI_NO_ENCONTRADO"));
        assertThat(vecinoRepository.findByTelefono("+51" + telefono)).isEmpty();
    }

    @Test
    void tresIntentosFallidosInvalidanElCodigoAunqueElRegistroFalle() throws Exception {
        String telefono = telefonoNuevo();
        String dni = dniNuevo();
        String codigo = solicitarOtp(telefono);
        String incorrecto = codigo.equals("000000") ? "111111" : "000000";

        registrar(dni, telefono, incorrecto, "-6.765000", "-79.850000")
                .andExpect(jsonPath("$.codigo").value("OTP_INCORRECTO"));
        registrar(dni, telefono, incorrecto, "-6.765000", "-79.850000")
                .andExpect(jsonPath("$.codigo").value("OTP_INCORRECTO"));
        registrar(dni, telefono, incorrecto, "-6.765000", "-79.850000")
                .andExpect(jsonPath("$.codigo").value("OTP_INTENTOS_AGOTADOS"));

        registrar(dni, telefono, codigo, "-6.765000", "-79.850000")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("OTP_NO_VIGENTE"));
    }

    @Test
    void cuartoOtpEnUnaHoraDevuelve429() throws Exception {
        String telefono = telefonoNuevo();
        for (int i = 0; i < 3; i++) {
            solicitarOtp(telefono);
        }
        postJson("/api/auth/otp/solicitar", """
                {"telefono":"%s"}""".formatted(telefono))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.codigo").value("OTP_LIMITE_ENVIOS"));
    }

    @Test
    void validacionDeEntradaDevuelveDetallesPorCampo() throws Exception {
        postJson("/api/auth/otp/solicitar", """
                {"telefono":"123"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.detalles[0].campo").value("telefono"));
    }

    // ------------------------------------------------------------------ login

    @Test
    void loginDeAdmin() throws Exception {
        String dni = crearAdmin();

        login(dni, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.rol").value("ADMIN"));
    }

    @Test
    void loginDeConductor() throws Exception {
        String dni = dniNuevo();
        personaRepository.save(new Conductor(dni, "Luis", "Ramos", "+51" + telefonoNuevo(),
                passwordEncoder.encode(PASSWORD), "Q" + dni));

        login(dni, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.rol").value("CONDUCTOR"));
    }

    @Test
    void passwordIncorrectoDevuelve401() throws Exception {
        login(crearAdmin(), "otra-clave")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
    }

    @Test
    void dniInexistenteDevuelve401() throws Exception {
        login(dniNuevo(), PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
    }

    @Test
    void personaInactivaNoPuedeIngresar() throws Exception {
        String dni = crearAdmin();
        var admin = personaRepository.findByDni(dni).orElseThrow();
        admin.setActivo(false);
        personaRepository.save(admin);

        login(dni, PASSWORD).andExpect(status().isUnauthorized());
    }

    // ----------------------------------------------------------- refresh / logout

    @Test
    void refreshRotaElTokenYElAnteriorYaNoSirve() throws Exception {
        String refresh1 = JsonPath.read(login(crearAdmin(), PASSWORD)
                .andReturn().getResponse().getContentAsString(), "$.refreshToken");

        String refresh2 = JsonPath.read(refrescar(refresh1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.usuario.rol").value("ADMIN"))
                .andReturn().getResponse().getContentAsString(), "$.refreshToken");
        assertThat(refresh2).isNotEqualTo(refresh1);

        // Reusar el token ya rotado revoca todas las sesiones de la persona
        refrescar(refresh1)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("REFRESH_INVALIDO"));
        refrescar(refresh2).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevocaElRefreshToken() throws Exception {
        String refresh = JsonPath.read(login(crearAdmin(), PASSWORD)
                .andReturn().getResponse().getContentAsString(), "$.refreshToken");

        postJson("/api/auth/logout", """
                {"refreshToken":"%s"}""".formatted(refresh))
                .andExpect(status().isNoContent());
        refrescar(refresh).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ roles

    @Test
    void accesoPorRol() throws Exception {
        String tokenAdmin = JsonPath.read(login(crearAdmin(), PASSWORD)
                .andReturn().getResponse().getContentAsString(), "$.accessToken");
        String telefono = telefonoNuevo();
        String tokenVecino = JsonPath.read(registrar(dniNuevo(), telefono, solicitarOtp(telefono),
                "-6.765000", "-79.850000").andReturn().getResponse().getContentAsString(), "$.accessToken");

        // Admin pasa la seguridad
        mockMvc.perform(get("/api/admin/sectores").header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());
        // Vecino no puede entrar a rutas de admin ni de conductor
        mockMvc.perform(get("/api/admin/sectores").header("Authorization", "Bearer " + tokenVecino))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
        mockMvc.perform(get("/api/conductor/rutas/hoy").header("Authorization", "Bearer " + tokenVecino))
                .andExpect(status().isForbidden());
        // Admin no reporta incidencias
        mockMvc.perform(post("/api/incidencias").header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isForbidden());
        // Token inválido -> 401
        mockMvc.perform(get("/api/vecino/me").header("Authorization", "Bearer basura"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    // ---------------------------------------------------------------- helpers

    private static String telefonoNuevo() {
        return "9" + String.format("%08d", SECUENCIA.incrementAndGet());
    }

    private static String dniNuevo() {
        return String.format("%08d", 40_000_000 + SECUENCIA.incrementAndGet());
    }

    private String crearAdmin() {
        String dni = dniNuevo();
        personaRepository.save(new Administrador(dni, "Ana", "Pérez", "+51" + telefonoNuevo(),
                passwordEncoder.encode(PASSWORD)));
        return dni;
    }

    private String solicitarOtp(String telefono) throws Exception {
        String body = postJson("/api/auth/otp/solicitar", """
                {"telefono":"%s"}""".formatted(telefono))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiraEn").exists())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.codigo");
    }

    private ResultActions registrar(String dni, String telefono, String codigo, String lat, String lng)
            throws Exception {
        return postJson("/api/auth/registro/vecino", """
                {"dni":"%s","telefono":"%s","codigo":"%s","direccionReferencia":"Mz. B Lt. 12, AA.HH. Prueba",
                 "latitud":%s,"longitud":%s}"""
                .formatted(dni, telefono, codigo, lat, lng));
    }

    private ResultActions login(String dni, String password) throws Exception {
        return postJson("/api/auth/login", """
                {"dni":"%s","password":"%s"}""".formatted(dni, password));
    }

    private ResultActions refrescar(String refreshToken) throws Exception {
        return postJson("/api/auth/refresh", """
                {"refreshToken":"%s"}""".formatted(refreshToken));
    }

    private ResultActions postJson(String url, String json) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
