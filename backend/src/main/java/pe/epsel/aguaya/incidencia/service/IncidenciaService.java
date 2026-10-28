package pe.epsel.aguaya.incidencia.service;

import java.time.Clock;
import java.time.OffsetDateTime;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.epsel.aguaya.common.dto.PaginaResponse;
import pe.epsel.aguaya.common.exception.NegocioException;
import pe.epsel.aguaya.common.seguridad.UsuarioAutenticado;
import pe.epsel.aguaya.incidencia.dto.IncidenciaResponse;
import pe.epsel.aguaya.incidencia.dto.ReportarIncidenciaRequest;
import pe.epsel.aguaya.incidencia.entity.EstadoIncidencia;
import pe.epsel.aguaya.incidencia.entity.Incidencia;
import pe.epsel.aguaya.incidencia.entity.TipoIncidencia;
import pe.epsel.aguaya.incidencia.mapper.IncidenciaMapper;
import pe.epsel.aguaya.incidencia.repository.IncidenciaRepository;
import pe.epsel.aguaya.monitoreo.dto.EventoRuta;
import pe.epsel.aguaya.monitoreo.service.PublicadorTiempoReal;
import pe.epsel.aguaya.persona.entity.EstadoDisponibilidad;
import pe.epsel.aguaya.persona.entity.Persona;
import pe.epsel.aguaya.persona.entity.Rol;
import pe.epsel.aguaya.persona.entity.Vecino;
import pe.epsel.aguaya.persona.repository.PersonaRepository;
import pe.epsel.aguaya.ruta.entity.Parada;
import pe.epsel.aguaya.ruta.entity.Ruta;
import pe.epsel.aguaya.ruta.entity.TipoParada;
import pe.epsel.aguaya.ruta.repository.ParadaRepository;
import pe.epsel.aguaya.ruta.repository.RutaRepository;

/**
 * Reporte de incidencias (RF06) por el conductor o un vecino, con alerta en tiempo real al administrador.
 */
@Service
@RequiredArgsConstructor
public class IncidenciaService {

    private final IncidenciaRepository incidenciaRepository;
    private final RutaRepository rutaRepository;
    private final ParadaRepository paradaRepository;
    private final PersonaRepository personaRepository;
    private final PublicadorTiempoReal publicador;
    private final Clock clock;

    /**
     * El conductor reporta sobre su ruta y el vecino sobre una ruta de su sector, cada uno con sus tipos.
     * Solo el conductor puede detener la ruta ({@code detieneRuta}), que pasa a INTERRUMPIDA.
     */
    @Transactional
    public IncidenciaResponse reportar(UsuarioAutenticado usuario, ReportarIncidenciaRequest r) {
        Persona persona = personaRepository.findById(usuario.id())
                .orElseThrow(() -> NegocioException.noEncontrado("Persona", usuario.id()));
        Ruta ruta = rutaVisible(r.rutaId(), persona);

        if (!r.tipo().permitidoPara(persona.getRol())) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "TIPO_NO_PERMITIDO",
                    "El tipo " + r.tipo() + " no corresponde a un " + persona.getRol().name().toLowerCase());
        }
        Parada parada = r.paradaId() != null ? paradaDeLaRuta(r.paradaId(), ruta, persona) : null;
        if (incidenciaRepository.existsByReportadoPorIdAndRutaIdAndTipoAndEstado(persona.getId(), ruta.getId(),
                r.tipo(), EstadoIncidencia.ABIERTA)) {
            throw NegocioException.conflicto("INCIDENCIA_DUPLICADA",
                    "Ya reportaste esta incidencia para la ruta; el administrador la está revisando");
        }

        OffsetDateTime ahora = OffsetDateTime.now(clock);
        if (Boolean.TRUE.equals(r.detieneRuta())) {
            if (persona.getRol() != Rol.CONDUCTOR) {
                throw new NegocioException(HttpStatus.BAD_REQUEST, "DETENER_NO_PERMITIDO",
                        "Solo el conductor puede detener la ruta");
            }
            ruta.interrumpir(ahora);
            ruta.getConductor().setEstadoDisponibilidad(EstadoDisponibilidad.DISPONIBLE);
            publicador.eventoRuta(new EventoRuta(EventoRuta.Tipo.RUTA_INTERRUMPIDA, ruta.getId(), null, ahora));
        }

        Incidencia incidencia = incidenciaRepository.save(new Incidencia(ruta, parada, persona, r.tipo(),
                r.descripcion() != null ? r.descripcion().trim() : null, ahora));
        IncidenciaResponse response = IncidenciaMapper.aResponse(incidencia);
        publicador.incidencia(response);
        return response;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<IncidenciaResponse> misIncidencias(Long personaId, Pageable pageable) {
        return PaginaResponse.de(incidenciaRepository.findByReportadoPorId(personaId, pageable),
                IncidenciaMapper::aResponse);
    }

    @Transactional(readOnly = true)
    public PaginaResponse<IncidenciaResponse> listar(EstadoIncidencia estado, TipoIncidencia tipo, Long rutaId,
                                                     Pageable pageable) {
        return PaginaResponse.de(incidenciaRepository.buscar(estado, tipo, rutaId, pageable),
                IncidenciaMapper::aResponse);
    }

    @Transactional(readOnly = true)
    public IncidenciaResponse obtener(Long id) {
        return IncidenciaMapper.aResponse(buscar(id));
    }

    @Transactional
    public IncidenciaResponse cerrar(Long id, Long adminId) {
        Incidencia incidencia = buscar(id);
        incidencia.cerrar(personaRepository.getReferenceById(adminId), OffsetDateTime.now(clock));
        return IncidenciaMapper.aResponse(incidencia);
    }

    private Incidencia buscar(Long id) {
        return incidenciaRepository.findById(id).orElseThrow(() -> NegocioException.noEncontrado("Incidencia", id));
    }

    /** Una ruta ajena (de otro conductor o de otro sector) se reporta como inexistente. */
    private Ruta rutaVisible(Long rutaId, Persona persona) {
        return rutaRepository.findById(rutaId)
                .filter(ruta -> switch (persona.getRol()) {
                    case CONDUCTOR -> ruta.getConductor() != null && ruta.getConductor().getId().equals(persona.getId());
                    case VECINO -> ruta.getSector().getId().equals(((Vecino) persona).getSector().getId());
                    case ADMIN -> false;
                })
                .orElseThrow(() -> NegocioException.noEncontrado("Ruta", rutaId));
    }

    /** La parada debe ser de la ruta; un vecino no puede reportar sobre el hogar de otro vecino. */
    private Parada paradaDeLaRuta(Long paradaId, Ruta ruta, Persona persona) {
        Parada parada = paradaRepository.findById(paradaId)
                .filter(p -> p.getRuta().getId().equals(ruta.getId()))
                .filter(p -> persona.getRol() != Rol.VECINO || p.getTipo() != TipoParada.HOGAR
                        || p.getVecino().getId().equals(persona.getId()))
                .orElse(null);
        if (parada == null) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "PARADA_NO_PERTENECE",
                    "La parada no pertenece a esta ruta");
        }
        return parada;
    }
}
