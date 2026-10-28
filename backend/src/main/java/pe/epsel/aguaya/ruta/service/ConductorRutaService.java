package pe.epsel.aguaya.ruta.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.epsel.aguaya.common.exception.NegocioException;
import pe.epsel.aguaya.monitoreo.dto.EventoRuta;
import pe.epsel.aguaya.monitoreo.service.PublicadorTiempoReal;
import pe.epsel.aguaya.notificacion.service.NotificacionService;
import pe.epsel.aguaya.persona.entity.EstadoDisponibilidad;
import pe.epsel.aguaya.ruta.dto.RutaDetalleResponse;
import pe.epsel.aguaya.ruta.entity.EstadoRuta;
import pe.epsel.aguaya.ruta.entity.Ruta;
import pe.epsel.aguaya.ruta.mapper.RutaMapper;
import pe.epsel.aguaya.ruta.repository.ParadaRepository;
import pe.epsel.aguaya.ruta.repository.RutaRepository;

/**
 * Lo que hace el conductor con su ruta del día: verla, iniciarla y finalizarla (regla 5).
 */
@Service
@RequiredArgsConstructor
public class ConductorRutaService {

    private final RutaRepository rutaRepository;
    private final ParadaRepository paradaRepository;
    private final PublicadorTiempoReal publicador;
    private final NotificacionService notificacionService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<RutaDetalleResponse> rutasDeHoy(Long conductorId) {
        return rutaRepository.findByConductorIdAndFechaProgramadaOrderByTurnoAsc(conductorId, LocalDate.now(clock))
                .stream()
                .map(r -> RutaMapper.aDetalle(r, paradaRepository.findDetalleByRutaId(r.getId())))
                .toList();
    }

    @Transactional
    public RutaDetalleResponse iniciar(Long rutaId, Long conductorId) {
        Ruta ruta = rutaDelConductor(rutaId, conductorId);
        if (!ruta.getFechaProgramada().equals(LocalDate.now(clock))) {
            throw NegocioException.conflicto("RUTA_NO_ES_HOY", "La ruta está programada para "
                    + ruta.getFechaProgramada() + "; solo se puede iniciar ese día");
        }
        if (ruta.getEstado() == EstadoRuta.PROGRAMADA
                && rutaRepository.existsByConductorIdAndEstado(conductorId, EstadoRuta.EN_CURSO)) {
            throw NegocioException.conflicto("CONDUCTOR_CON_RUTA_EN_CURSO",
                    "Finalice la ruta en curso antes de iniciar otra");
        }
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        ruta.iniciar(ahora);
        ruta.getConductor().setEstadoDisponibilidad(EstadoDisponibilidad.EN_RUTA);
        publicador.eventoRuta(new EventoRuta(EventoRuta.Tipo.RUTA_INICIADA, rutaId, null, ahora));
        notificacionService.rutaIniciada(ruta);
        return RutaMapper.aDetalle(ruta, paradaRepository.findDetalleByRutaId(rutaId));
    }

    @Transactional
    public RutaDetalleResponse finalizar(Long rutaId, Long conductorId) {
        Ruta ruta = rutaDelConductor(rutaId, conductorId);
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        ruta.completar(ahora);
        ruta.getConductor().setEstadoDisponibilidad(EstadoDisponibilidad.DISPONIBLE);
        publicador.eventoRuta(new EventoRuta(EventoRuta.Tipo.RUTA_COMPLETADA, rutaId, null, ahora));
        return RutaMapper.aDetalle(ruta, paradaRepository.findDetalleByRutaId(rutaId));
    }

    /** Una ruta ajena se reporta como inexistente para no revelar rutas de otros conductores. */
    public Ruta rutaDelConductor(Long rutaId, Long conductorId) {
        return rutaRepository.findById(rutaId)
                .filter(r -> r.getConductor() != null && r.getConductor().getId().equals(conductorId))
                .orElseThrow(() -> NegocioException.noEncontrado("Ruta", rutaId));
    }
}
