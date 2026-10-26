package pe.epsel.aguaya.persona.service;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.epsel.aguaya.auth.repository.RefreshTokenRepository;
import pe.epsel.aguaya.common.dominio.Turno;
import pe.epsel.aguaya.common.dto.PaginaResponse;
import pe.epsel.aguaya.common.exception.NegocioException;
import pe.epsel.aguaya.common.util.Telefono;
import pe.epsel.aguaya.persona.dto.ConductorActualizarRequest;
import pe.epsel.aguaya.persona.dto.ConductorCrearRequest;
import pe.epsel.aguaya.persona.dto.ConductorResponse;
import pe.epsel.aguaya.persona.entity.Conductor;
import pe.epsel.aguaya.persona.entity.EstadoDisponibilidad;
import pe.epsel.aguaya.persona.mapper.ConductorMapper;
import pe.epsel.aguaya.persona.repository.ConductorRepository;
import pe.epsel.aguaya.persona.repository.PersonaRepository;

/**
 * Cuentas de conductor gestionadas por el administrador.
 */
@Service
@RequiredArgsConstructor
public class ConductorService {

    private final ConductorRepository conductorRepository;
    private final PersonaRepository personaRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public PaginaResponse<ConductorResponse> listar(EstadoDisponibilidad estado, Turno turno, Boolean activo,
                                                    Pageable pageable) {
        return PaginaResponse.de(conductorRepository.buscar(estado, turno, activo, pageable),
                ConductorMapper::aResponse);
    }

    @Transactional(readOnly = true)
    public ConductorResponse obtener(Long id) {
        return ConductorMapper.aResponse(buscar(id));
    }

    @Transactional
    public ConductorResponse crear(ConductorCrearRequest r) {
        String telefono = Telefono.normalizar(r.telefono());
        String licencia = normalizarLicencia(r.licencia());
        if (personaRepository.existsByDni(r.dni())) {
            throw NegocioException.conflicto("DNI_REGISTRADO", "Ya existe una cuenta con ese DNI");
        }
        if (personaRepository.existsByTelefono(telefono)) {
            throw telefonoDuplicado();
        }
        if (conductorRepository.existsByLicencia(licencia)) {
            throw licenciaDuplicada();
        }

        Conductor conductor = new Conductor(r.dni(), r.nombres().trim(), r.apellidos().trim(), telefono,
                passwordEncoder.encode(r.password()), licencia);
        conductor.setEmail(r.email());
        conductor.setTurno(r.turno());
        return ConductorMapper.aResponse(conductorRepository.save(conductor));
    }

    @Transactional
    public ConductorResponse actualizar(Long id, ConductorActualizarRequest r) {
        Conductor conductor = buscar(id);
        String telefono = Telefono.normalizar(r.telefono());
        String licencia = normalizarLicencia(r.licencia());
        if (personaRepository.existsByTelefonoAndIdNot(telefono, id)) {
            throw telefonoDuplicado();
        }
        if (conductorRepository.existsByLicenciaAndIdNot(licencia, id)) {
            throw licenciaDuplicada();
        }
        if (r.estadoDisponibilidad() == EstadoDisponibilidad.EN_RUTA) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "ESTADO_NO_PERMITIDO",
                    "El estado EN_RUTA lo asigna el sistema al iniciar una ruta");
        }

        conductor.setNombres(r.nombres().trim());
        conductor.setApellidos(r.apellidos().trim());
        conductor.setTelefono(telefono);
        conductor.setEmail(r.email());
        conductor.setLicencia(licencia);
        conductor.setTurno(r.turno());
        if (r.estadoDisponibilidad() != null) {
            conductor.setEstadoDisponibilidad(r.estadoDisponibilidad());
        }
        if (r.password() != null) {
            conductor.setPasswordHash(passwordEncoder.encode(r.password()));
            refreshTokenRepository.revocarTodosDeLaPersona(id);
        }
        if (Boolean.FALSE.equals(r.activo())) {
            desactivar(conductor);
        } else if (Boolean.TRUE.equals(r.activo())) {
            conductor.setActivo(true);
        }
        return ConductorMapper.aResponse(conductor);
    }

    /** Baja lógica: no puede ingresar, queda INACTIVO y se cierran sus sesiones. */
    @Transactional
    public void desactivar(Long id) {
        desactivar(buscar(id));
    }

    private void desactivar(Conductor conductor) {
        conductor.setActivo(false);
        conductor.setEstadoDisponibilidad(EstadoDisponibilidad.INACTIVO);
        refreshTokenRepository.revocarTodosDeLaPersona(conductor.getId());
    }

    private Conductor buscar(Long id) {
        return conductorRepository.findById(id).orElseThrow(() -> NegocioException.noEncontrado("Conductor", id));
    }

    private static String normalizarLicencia(String licencia) {
        return licencia.trim().toUpperCase();
    }

    private static NegocioException telefonoDuplicado() {
        return NegocioException.conflicto("TELEFONO_REGISTRADO", "Ya existe una cuenta con ese teléfono");
    }

    private static NegocioException licenciaDuplicada() {
        return NegocioException.conflicto("LICENCIA_DUPLICADA", "Ya existe un conductor con esa licencia");
    }
}
