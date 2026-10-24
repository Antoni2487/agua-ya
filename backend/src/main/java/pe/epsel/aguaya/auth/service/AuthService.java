package pe.epsel.aguaya.auth.service;

import java.util.Comparator;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.epsel.aguaya.auth.dto.AuthResponse;
import pe.epsel.aguaya.auth.dto.LoginRequest;
import pe.epsel.aguaya.auth.dto.RegistroVecinoRequest;
import pe.epsel.aguaya.auth.dto.SolicitarOtpRequest;
import pe.epsel.aguaya.auth.dto.SolicitarOtpResponse;
import pe.epsel.aguaya.common.exception.NegocioException;
import pe.epsel.aguaya.common.util.Haversine;
import pe.epsel.aguaya.common.util.Telefono;
import pe.epsel.aguaya.persona.entity.Persona;
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
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    /** Envía el código que confirma el teléfono antes del registro. */
    @Transactional
    public SolicitarOtpResponse solicitarOtp(SolicitarOtpRequest request) {
        String telefono = Telefono.normalizar(request.telefono());
        validarDisponibles(request.dni(), telefono);

        ProveedorOtp.OtpEnviado otp = proveedorOtp.enviar(telefono);
        return new SolicitarOtpResponse("Código enviado por SMS", otp.expiraEn(), otp.codigo());
    }

    /**
     * Regla 8: el sector se asigna al más cercano (por centro) y el vecino queda activo
     * de inmediato; la sesión se abre en la misma respuesta.
     */
    @Transactional
    public AuthResponse registrarVecino(RegistroVecinoRequest request) {
        String telefono = Telefono.normalizar(request.telefono());
        validarDisponibles(request.dni(), telefono);
        Sector sector = sectorMasCercano(request.latitud().doubleValue(), request.longitud().doubleValue());

        proveedorOtp.verificar(telefono, request.codigo());

        Vecino vecino = new Vecino(request.dni(), request.nombres().trim(), request.apellidos().trim(), telefono,
                passwordEncoder.encode(request.password()));
        vecino.setDireccionReferencia(request.direccionReferencia());
        vecino.setLatitud(request.latitud());
        vecino.setLongitud(request.longitud());
        vecino.setSector(sector);
        vecinoRepository.save(vecino);

        return emitirSesion(vecino);
    }

    /** Login con DNI + contraseña para los tres roles. */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        Persona persona = personaRepository.findByDni(request.dni()).orElse(null);

        String hash = persona != null ? persona.getPasswordHash() : hashFicticio;
        boolean passwordOk = passwordEncoder.matches(request.password(), hash);

        if (persona == null || !passwordOk || !persona.isActivo()) {
            throw new NegocioException(HttpStatus.UNAUTHORIZED, "CREDENCIALES_INVALIDAS", "DNI o contraseña incorrectos");
        }
        return emitirSesion(persona);
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
