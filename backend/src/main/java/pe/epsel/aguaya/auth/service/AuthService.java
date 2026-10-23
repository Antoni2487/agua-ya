package pe.epsel.aguaya.auth.service;

import java.util.Comparator;
import java.util.Set;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.epsel.aguaya.auth.dto.AuthResponse;
import pe.epsel.aguaya.auth.dto.LoginRequest;
import pe.epsel.aguaya.auth.dto.LoginVecinoRequest;
import pe.epsel.aguaya.auth.dto.RegistroVecinoRequest;
import pe.epsel.aguaya.auth.dto.SolicitarOtpRequest;
import pe.epsel.aguaya.auth.dto.SolicitarOtpResponse;
import pe.epsel.aguaya.auth.entity.PropositoOtp;
import pe.epsel.aguaya.common.exception.NegocioException;
import pe.epsel.aguaya.common.util.Haversine;
import pe.epsel.aguaya.common.util.Telefono;
import pe.epsel.aguaya.sector.entity.Sector;
import pe.epsel.aguaya.sector.repository.SectorRepository;
import pe.epsel.aguaya.usuario.entity.Rol;
import pe.epsel.aguaya.usuario.entity.Usuario;
import pe.epsel.aguaya.usuario.entity.Vecino;
import pe.epsel.aguaya.usuario.mapper.UsuarioMapper;
import pe.epsel.aguaya.usuario.repository.UsuarioRepository;
import pe.epsel.aguaya.usuario.repository.VecinoRepository;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Set<Rol> ROLES_CON_PASSWORD = Set.of(Rol.ADMIN, Rol.CONDUCTOR);

    /** Hash BCrypt de referencia para igualar el tiempo de respuesta cuando el DNI no existe. */
    private final String hashFicticio = new BCryptPasswordEncoder().encode("aguaya-hash-ficticio");

    private final UsuarioRepository usuarioRepository;
    private final VecinoRepository vecinoRepository;
    private final SectorRepository sectorRepository;
    private final OtpService otpService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public SolicitarOtpResponse solicitarOtp(SolicitarOtpRequest request) {
        String telefono = Telefono.normalizar(request.telefono());

        if (request.proposito() == PropositoOtp.REGISTRO) {
            validarDisponibles(request.dni(), telefono);
        } else {
            vecinoRepository.findByTelefono(telefono)
                    .filter(Usuario::isActivo)
                    .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "VECINO_NO_ENCONTRADO",
                            "No hay un vecino registrado con ese teléfono"));
        }

        OtpService.OtpEmitido otp = otpService.emitir(telefono, request.proposito());
        return new SolicitarOtpResponse("Código enviado por SMS", otp.expiraEn(), otp.codigo());
    }

    /**
     * Regla 8: el sector se asigna al más cercano (por centro) y el vecino queda PENDIENTE_VALIDACION.
     */
    @Transactional
    public AuthResponse registrarVecino(RegistroVecinoRequest request) {
        String telefono = Telefono.normalizar(request.telefono());
        validarDisponibles(request.dni(), telefono);
        Sector sector = sectorMasCercano(request.latitud().doubleValue(), request.longitud().doubleValue());

        otpService.verificar(telefono, PropositoOtp.REGISTRO, request.codigo());

        Vecino vecino = new Vecino(request.dni(), request.nombres().trim(), request.apellidos().trim(), telefono);
        vecino.setDireccionReferencia(request.direccionReferencia());
        vecino.setLatitud(request.latitud());
        vecino.setLongitud(request.longitud());
        vecino.setSector(sector);
        vecinoRepository.save(vecino);

        return emitirSesion(vecino);
    }

    @Transactional
    public AuthResponse loginVecino(LoginVecinoRequest request) {
        String telefono = Telefono.normalizar(request.telefono());
        Vecino vecino = vecinoRepository.findByTelefono(telefono)
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "VECINO_NO_ENCONTRADO",
                        "No hay un vecino registrado con ese teléfono"));

        otpService.verificar(telefono, PropositoOtp.LOGIN, request.codigo());
        return emitirSesion(vecino);
    }

    /** Login con DNI + contraseña, solo para administrador y conductor. */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByDni(request.dni())
                .filter(u -> ROLES_CON_PASSWORD.contains(u.getRol()))
                .orElse(null);

        String hash = usuario != null && usuario.getPasswordHash() != null ? usuario.getPasswordHash() : hashFicticio;
        boolean passwordOk = passwordEncoder.matches(request.password(), hash);

        if (usuario == null || !passwordOk || !usuario.isActivo()) {
            throw new NegocioException(HttpStatus.UNAUTHORIZED, "CREDENCIALES_INVALIDAS", "DNI o contraseña incorrectos");
        }
        return emitirSesion(usuario);
    }

    @Transactional
    public AuthResponse refrescar(String refreshToken) {
        Long usuarioId = refreshTokenService.consumir(refreshToken);
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> NegocioException.noEncontrado("Usuario", usuarioId));
        return emitirSesion(usuario);
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revocar(refreshToken);
    }

    private AuthResponse emitirSesion(Usuario usuario) {
        return new AuthResponse(
                jwtService.generarAccessToken(usuario),
                refreshTokenService.emitir(usuario),
                "Bearer",
                jwtService.vigenciaSegundos(),
                UsuarioMapper.aResumen(usuario));
    }

    private void validarDisponibles(String dni, String telefono) {
        if (dni != null && usuarioRepository.existsByDni(dni)) {
            throw NegocioException.conflicto("DNI_REGISTRADO", "Ya existe un usuario con ese DNI");
        }
        if (usuarioRepository.existsByTelefono(telefono)) {
            throw NegocioException.conflicto("TELEFONO_REGISTRADO", "Ya existe un usuario con ese teléfono");
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
