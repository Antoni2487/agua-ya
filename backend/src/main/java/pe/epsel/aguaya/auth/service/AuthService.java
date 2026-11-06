package pe.epsel.aguaya.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Comparator;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import pe.epsel.aguaya.auth.dto.AuthResponse;
import pe.epsel.aguaya.auth.dto.LoginCodigoRequest;
import pe.epsel.aguaya.auth.dto.LoginRequest;
import pe.epsel.aguaya.auth.dto.RegistroVecinoRequest;
import pe.epsel.aguaya.auth.dto.RestablecerClaveRequest;
import pe.epsel.aguaya.auth.dto.SolicitarCodigoLoginRequest;
import pe.epsel.aguaya.auth.dto.SolicitarCodigoLoginResponse;
import pe.epsel.aguaya.auth.dto.SolicitarOtpRequest;
import pe.epsel.aguaya.auth.dto.SolicitarOtpResponse;
import pe.epsel.aguaya.common.exception.NegocioException;
import pe.epsel.aguaya.common.util.Coordenada;
import pe.epsel.aguaya.common.util.Haversine;
import pe.epsel.aguaya.common.util.Telefono;
import pe.epsel.aguaya.persona.entity.Persona;
import pe.epsel.aguaya.persona.entity.Rol;
import pe.epsel.aguaya.persona.entity.Vecino;
import pe.epsel.aguaya.persona.mapper.PersonaMapper;
import pe.epsel.aguaya.persona.repository.PersonaRepository;
import pe.epsel.aguaya.persona.repository.VecinoRepository;
import pe.epsel.aguaya.sector.entity.Sector;
import pe.epsel.aguaya.sector.repository.SectorRepository;

@Service
@RequiredArgsConstructor
public class AuthService {

    /** Hash BCrypt de referencia para igualar el tiempo de respuesta cuando el DNI no existe. */
    private final String hashFicticio = new BCryptPasswordEncoder().encode("aguaya-hash-ficticio");

    private final PersonaRepository personaRepository;
    private final VecinoRepository vecinoRepository;
    private final SectorRepository sectorRepository;
    private final ProveedorOtp proveedorOtp;
    private final ProveedorDni proveedorDni;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transaccion;
    private final Clock clock;

    /** Intentos fallidos seguidos antes de bloquear el ingreso con contraseña o clave. */
    static final int MAX_INTENTOS = 5;
    static final Duration BLOQUEO = Duration.ofMinutes(15);

    /** Envía el código que confirma el teléfono antes del registro. */
    @Transactional
    public SolicitarOtpResponse solicitarOtp(SolicitarOtpRequest request) {
        String telefono = Telefono.normalizar(request.telefono());
        validarDisponibles(request.dni(), telefono);

        ProveedorOtp.OtpEnviado otp = proveedorOtp.enviar(telefono);
        return new SolicitarOtpResponse("Código enviado por SMS", otp.expiraEn(), otp.codigo());
    }

    /**
     * Regla 8: el sector se asigna al más cercano (por centro) y el vecino queda activo de inmediato; la sesión
     * se abre en la misma respuesta. Primero se verifica el código (solo un teléfono confirmado gasta una
     * consulta de DNI) y luego se obtienen los nombres del DNI. La consulta puede tardar más de 20 s, por eso
     * corre fuera de la transacción que guarda al vecino (no retiene una conexión de la BD mientras espera).
     */
    public AuthResponse registrarVecino(RegistroVecinoRequest request) {
        String telefono = Telefono.normalizar(request.telefono());
        validarDisponibles(request.dni(), telefono);
        double latitud = request.latitud().doubleValue();
        double longitud = request.longitud().doubleValue();
        sectorMasCercano(latitud, longitud);

        proveedorOtp.verificar(telefono, request.codigo());
        ProveedorDni.DatosDni datos = proveedorDni.consultar(request.dni());

        return transaccion.execute(estado -> {
            // Otra persona pudo registrarse con el mismo DNI o teléfono mientras se consultaba el DNI
            validarDisponibles(request.dni(), telefono);
            Vecino vecino = new Vecino(request.dni(), datos.nombres(), datos.apellidos(), telefono);
            vecino.setDireccionReferencia(request.direccionReferencia());
            vecino.setLatitud(Coordenada.normalizar(request.latitud()));
            vecino.setLongitud(Coordenada.normalizar(request.longitud()));
            vecino.setSector(sectorMasCercano(latitud, longitud));
            vecino.setPasswordHash(passwordEncoder.encode(request.clave()));
            vecinoRepository.save(vecino);
            return emitirSesion(vecino);
        });
    }

    /**
     * Login con DNI + contraseña (administrador y conductor) o DNI + clave de 4 dígitos (vecino). Tras
     * {@link #MAX_INTENTOS} fallos seguidos la cuenta no acepta ingresos durante {@link #BLOQUEO}; el vecino puede
     * restablecer su clave con el código SMS. Los fallos se guardan aunque la respuesta sea un error.
     */
    @Transactional(noRollbackFor = NegocioException.class)
    public AuthResponse login(LoginRequest request) {
        Persona persona = personaRepository.findByDni(request.dni()).orElse(null);
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        if (persona != null && persona.getBloqueadoHasta() != null && persona.getBloqueadoHasta().isAfter(ahora)) {
            throw new NegocioException(HttpStatus.TOO_MANY_REQUESTS, "CUENTA_BLOQUEADA",
                    "Demasiados intentos. Espera 15 minutos o restablece tu clave con un código SMS");
        }

        String hash = persona != null && persona.getPasswordHash() != null ? persona.getPasswordHash() : hashFicticio;
        boolean passwordOk = passwordEncoder.matches(request.password(), hash);

        if (persona == null || persona.getPasswordHash() == null || !passwordOk || !persona.isActivo()) {
            if (persona != null && !passwordOk) {
                registrarFallo(persona, ahora);
            }
            throw new NegocioException(HttpStatus.UNAUTHORIZED, "CREDENCIALES_INVALIDAS",
                    persona != null && persona.getRol() == Rol.VECINO ? "DNI o clave incorrectos"
                            : "DNI o contraseña incorrectos");
        }
        persona.setIntentosFallidos((short) 0);
        persona.setBloqueadoHasta(null);
        return emitirSesion(persona);
    }

    private void registrarFallo(Persona persona, OffsetDateTime ahora) {
        int intentos = persona.getIntentosFallidos() + 1;
        if (intentos >= MAX_INTENTOS) {
            persona.setBloqueadoHasta(ahora.plus(BLOQUEO));
            intentos = 0;
        }
        persona.setIntentosFallidos((short) intentos);
    }

    /** El vecino que olvidó su clave la cambia con el código SMS que llega a su celular y queda con sesión. */
    @Transactional
    public AuthResponse restablecerClave(RestablecerClaveRequest request) {
        Persona vecino = vecinoParaIngresar(request.dni());
        proveedorOtp.verificar(vecino.getTelefono(), request.codigo());
        vecino.setPasswordHash(passwordEncoder.encode(request.clave()));
        vecino.setIntentosFallidos((short) 0);
        vecino.setBloqueadoHasta(null);
        return emitirSesion(vecino);
    }

    /** El vecino pide el código para ingresar; llega al celular con el que se registró. */
    @Transactional
    public SolicitarCodigoLoginResponse solicitarCodigoLogin(SolicitarCodigoLoginRequest request) {
        Persona vecino = vecinoParaIngresar(request.dni());
        ProveedorOtp.OtpEnviado otp = proveedorOtp.enviar(vecino.getTelefono());
        return new SolicitarCodigoLoginResponse("Código enviado por SMS", enmascarar(vecino.getTelefono()),
                otp.expiraEn(), otp.codigo());
    }

    /** Ingreso del vecino con DNI + código SMS. */
    @Transactional
    public AuthResponse loginConCodigo(LoginCodigoRequest request) {
        Persona vecino = vecinoParaIngresar(request.dni());
        proveedorOtp.verificar(vecino.getTelefono(), request.codigo());
        return emitirSesion(vecino);
    }

    @Transactional
    public AuthResponse refrescar(String refreshToken) {
        Long personaId = refreshTokenService.consumir(refreshToken);
        Persona persona = personaRepository.findById(personaId)
                .orElseThrow(() -> NegocioException.noEncontrado("Persona", personaId));
        return emitirSesion(persona);
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revocar(refreshToken);
    }

    private Persona vecinoParaIngresar(String dni) {
        Persona persona = personaRepository.findByDni(dni)
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "DNI_NO_REGISTRADO",
                        "Ese DNI no tiene una cuenta. Regístrate primero"));
        if (persona.getRol() != Rol.VECINO) {
            throw NegocioException.conflicto("INGRESO_CON_PASSWORD", "Ingresa con tu DNI y contraseña");
        }
        if (!persona.isActivo()) {
            throw new NegocioException(HttpStatus.FORBIDDEN, "CUENTA_INACTIVA",
                    "Tu cuenta está desactivada. Comunícate con EPSEL");
        }
        return persona;
    }

    /** "+51987654789" → "*** *** 789". */
    static String enmascarar(String telefono) {
        return "*** *** " + telefono.substring(telefono.length() - 3);
    }

    private AuthResponse emitirSesion(Persona persona) {
        return new AuthResponse(
                jwtService.generarAccessToken(persona),
                refreshTokenService.emitir(persona),
                "Bearer",
                jwtService.vigenciaSegundos(),
                PersonaMapper.aResumen(persona));
    }

    private void validarDisponibles(String dni, String telefono) {
        if (dni != null && personaRepository.existsByDni(dni)) {
            throw NegocioException.conflicto("DNI_REGISTRADO", "Ya existe una cuenta con ese DNI");
        }
        if (personaRepository.existsByTelefono(telefono)) {
            throw NegocioException.conflicto("TELEFONO_REGISTRADO", "Ya existe una cuenta con ese teléfono");
        }
    }

    private Sector sectorMasCercano(double latitud, double longitud) {
        return sectorRepository.findByActivoTrue().stream()
                .min(Comparator.comparingDouble(s -> Haversine.distanciaMetros(
                        latitud, longitud, s.getLatCentro().doubleValue(), s.getLngCentro().doubleValue())))
                .orElseThrow(() -> NegocioException.conflicto("SIN_SECTORES",
                        "No hay sectores activos para asignar al vecino"));
    }
}
