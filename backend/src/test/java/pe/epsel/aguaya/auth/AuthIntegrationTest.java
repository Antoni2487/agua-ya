package pe.epsel.aguaya.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
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
import pe.epsel.aguaya.sector.entity.Modalidad;
import pe.epsel.aguaya.sector.entity.Sector;
import pe.epsel.aguaya.sector.repository.SectorRepository;
import pe.epsel.aguaya.usuario.entity.Administrador;
import pe.epsel.aguaya.usuario.entity.Conductor;
import pe.epsel.aguaya.usuario.entity.Vecino;
import pe.epsel.aguaya.usuario.repository.UsuarioRepository;
import pe.epsel.aguaya.usuario.repository.VecinoRepository;

@SpringBootTest(properties = "aguaya.otp.exponer-codigo=true")
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class AuthIntegrationTest {

    /** Cada test usa DNI y teléfono propios para no interferir con los demás. */
    private static final AtomicInteger SECUENCIA = new AtomicInteger(1000);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SectorRepository sectorRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private VecinoRepository vecinoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Sector sectorChiclayo;
    private Sector sectorMonsefu;

    @BeforeEach
    void sectores() {
        if (sectorRepository.count() == 0) {
            sectorRepository.save(new Sector("Cerropón", "Chiclayo", Modalidad.PERMANENTE, 300, 4,
                    new BigDecimal("-6.765000"), new BigDecimal("-79.850000")));
            sectorRepository.save(new Sector("Callanca", "Monsefú", Modalidad.PERMANENTE, 200, 3,
                    new BigDecimal("-6.880000"), new BigDecimal("-79.870000")));
        }
        sectorChiclayo = sectorRepository.findAll().stream().filter(s -> s.getDistrito().equals("Chiclayo"))
                .findFirst().orElseThrow();
        sectorMonsefu = sectorRepository.findAll().stream().filter(s -> s.getDistrito().equals("Monsefú"))
                .findFirst().orElseThrow();
    }

    // ------------------------------------------------------------------ vecino

    @Test
    void registroDeVecinoAsignaSectorMasCercanoYQuedaPendiente() throws Exception {
        String telefono = telefonoNuevo();
        String dni = dniNuevo();
        String codigo = solicitarOtp(telefono, "REGISTRO");

        // Pin cerca de Monsefú
        registrar(dni, telefono, codigo, "-6.875000", "-79.868000")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tipoToken").value("Bearer"))
                .andExpect(jsonPath("$.usuario.rol").value("VECINO"))
                .andExpect(jsonPath("$.usuario.estadoVecino").value("PENDIENTE_VALIDACION"));

        Vecino vecino = vecinoRepository.findByTelefono("+51" + telefono).orElseThrow();
        assertThat(vecino.getDni()).isEqualTo(dni);
        assertThat(vecino.getPasswordHash()).isNull();
        assertThat(vecino.getSector().getId()).isEqualTo(sectorMonsefu.getId());
        assertThat(vecino.getSector().getId()).isNotEqualTo(sectorChiclayo.getId());
    }

    @Test
    void registroConDniDuplicadoDevuelve409() throws Exception {
        String telefono = telefonoNuevo();
        String dni = dniNuevo();
        registrar(dni, telefono, solicitarOtp(telefono, "REGISTRO"), "-6.765000", "-79.850000")
                .andExpect(status().isCreated());

        String otroTelefono = telefonoNuevo();
        registrar(dni, otroTelefono, solicitarOtp(otroTelefono, "REGISTRO"), "-6.765000", "-79.850000")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("DNI_REGISTRADO"));
    }

    @Test
    void solicitarOtpDeRegistroConTelefonoYaRegistradoDevuelve409() throws Exception {
        String telefono = registrarVecino();

        postJson("/api/auth/otp/solicitar", """
                {"telefono":"%s","proposito":"REGISTRO"}""".formatted(telefono))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TELEFONO_REGISTRADO"));
    }

    @Test
    void tresIntentosFallidosInvalidanElCodigoAunqueElRegistroFalle() throws Exception {
        String telefono = telefonoNuevo();
        String dni = dniNuevo();
        String codigo = solicitarOtp(telefono, "REGISTRO");
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
            solicitarOtp(telefono, "REGISTRO");
        }
        postJson("/api/auth/otp/solicitar", """
                {"telefono":"%s","proposito":"REGISTRO"}""".formatted(telefono))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.codigo").value("OTP_LIMITE_ENVIOS"));
    }

    @Test
    void loginDeVecinoConOtp() throws Exception {
        String telefono = registrarVecino();
        String codigo = solicitarOtp(telefono, "LOGIN");

        postJson("/api/auth/login/vecino", """
                {"telefono":"%s","codigo":"%s"}""".formatted(telefono, codigo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.usuario.rol").value("VECINO"));
    }

    @Test
    void otpDeLoginParaTelefonoNoRegistradoDevuelve404() throws Exception {
        postJson("/api/auth/otp/solicitar", """
                {"telefono":"%s","proposito":"LOGIN"}""".formatted(telefonoNuevo()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("VECINO_NO_ENCONTRADO"));
    }

    @Test
    void validacionDeEntradaDevuelveDetallesPorCampo() throws Exception {
        postJson("/api/auth/otp/solicitar", """
                {"telefono":"123","proposito":"REGISTRO"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.detalles[0].campo").value("telefono"));
    }

    // ------------------------------------------------------- admin / conductor

    @Test
    void loginDeAdminConDniYPassword() throws Exception {
        String dni = crearAdmin("Secreta123");

        login(dni, "Secreta123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.rol").value("ADMIN"))
                .andExpect(jsonPath("$.usuario.estadoVecino").doesNotExist());
    }

    @Test
    void loginDeConductor() throws Exception {
        String dni = dniNuevo();
        Conductor conductor = new Conductor(dni, "Luis", "Ramos", "+51" + telefonoNuevo(), "Q" + dni);
        conductor.setPasswordHash(passwordEncoder.encode("Conductor1"));
        usuarioRepository.save(conductor);

        login(dni, "Conductor1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.rol").value("CONDUCTOR"));
    }

    @Test
    void passwordIncorrectoDevuelve401() throws Exception {
        String dni = crearAdmin("Secreta123");

        login(dni, "otra")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
    }

    @Test
    void usuarioInactivoNoPuedeIngresar() throws Exception {
        String dni = crearAdmin("Secreta123");
        var admin = usuarioRepository.findByDni(dni).orElseThrow();
        admin.setActivo(false);
        usuarioRepository.save(admin);

        login(dni, "Secreta123").andExpect(status().isUnauthorized());
    }

    @Test
    void vecinoNoPuedeIngresarConPassword() throws Exception {
        String telefono = registrarVecino();
        String dni = vecinoRepository.findByTelefono("+51" + telefono).orElseThrow().getDni();

        login(dni, "cualquiera")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
    }

    // ----------------------------------------------------------- refresh / logout

    @Test
    void refreshRotaElTokenYElAnteriorYaNoSirve() throws Exception {
        String refresh1 = JsonPath.read(login(crearAdmin("Secreta123"), "Secreta123")
                .andReturn().getResponse().getContentAsString(), "$.refreshToken");

        String refresh2 = JsonPath.read(refrescar(refresh1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.usuario.rol").value("ADMIN"))
                .andReturn().getResponse().getContentAsString(), "$.refreshToken");
        assertThat(refresh2).isNotEqualTo(refresh1);

        // Reusar el token ya rotado revoca todas las sesiones del usuario
        refrescar(refresh1)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("REFRESH_INVALIDO"));
        refrescar(refresh2).andExpect(status().isUnauthorized());
    }

    @Test
    void refreshDeVecinoMantieneSuEstado() throws Exception {
        String telefono = telefonoNuevo();
        String refresh = JsonPath.read(registrar(dniNuevo(), telefono, solicitarOtp(telefono, "REGISTRO"),
                "-6.765000", "-79.850000").andReturn().getResponse().getContentAsString(), "$.refreshToken");

        refrescar(refresh)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.estadoVecino").value("PENDIENTE_VALIDACION"));
    }

    @Test
    void logoutRevocaElRefreshToken() throws Exception {
        String refresh = JsonPath.read(login(crearAdmin("Secreta123"), "Secreta123")
                .andReturn().getResponse().getContentAsString(), "$.refreshToken");

        postJson("/api/auth/logout", """
                {"refreshToken":"%s"}""".formatted(refresh))
                .andExpect(status().isNoContent());
        refrescar(refresh).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ roles

    @Test
    void accesoPorRol() throws Exception {
        String tokenAdmin = JsonPath.read(login(crearAdmin("Secreta123"), "Secreta123")
                .andReturn().getResponse().getContentAsString(), "$.accessToken");
        String telefono = telefonoNuevo();
        String tokenVecino = JsonPath.read(registrar(dniNuevo(), telefono, solicitarOtp(telefono, "REGISTRO"),
                "-6.765000", "-79.850000").andReturn().getResponse().getContentAsString(), "$.accessToken");

        // Admin pasa la seguridad (el endpoint aún no existe -> 404)
        mockMvc.perform(get("/api/admin/sectores").header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isNotFound());
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

    private String crearAdmin(String password) {
        String dni = dniNuevo();
        Administrador admin = new Administrador(dni, "Ana", "Pérez", "+51" + telefonoNuevo());
        admin.setPasswordHash(passwordEncoder.encode(password));
        usuarioRepository.save(admin);
        return dni;
    }

    /** Registra un vecino nuevo y devuelve su teléfono (9 dígitos). */
    private String registrarVecino() throws Exception {
        String telefono = telefonoNuevo();
        registrar(dniNuevo(), telefono, solicitarOtp(telefono, "REGISTRO"), "-6.765000", "-79.850000")
                .andExpect(status().isCreated());
        return telefono;
    }

    private String solicitarOtp(String telefono, String proposito) throws Exception {
        String body = postJson("/api/auth/otp/solicitar", """
                {"telefono":"%s","proposito":"%s"}""".formatted(telefono, proposito))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiraEn").exists())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.codigo");
    }

    private ResultActions registrar(String dni, String telefono, String codigo, String lat, String lng)
            throws Exception {
        return postJson("/api/auth/registro/vecino", """
                {"nombres":"María","apellidos":"Chávez","dni":"%s","telefono":"%s","codigo":"%s",
                 "direccionReferencia":"Mz. B Lt. 12, AA.HH. Prueba","latitud":%s,"longitud":%s}"""
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
