package pe.epsel.aguaya.ruta.service;

import java.time.Clock;
import java.time.OffsetDateTime;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.epsel.aguaya.common.exception.NegocioException;
import pe.epsel.aguaya.monitoreo.dto.EventoRuta;
import pe.epsel.aguaya.monitoreo.service.PublicadorTiempoReal;
import pe.epsel.aguaya.puntollenado.entity.PuntoLlenado;
import pe.epsel.aguaya.puntollenado.repository.PuntoLlenadoRepository;
import pe.epsel.aguaya.ruta.dto.IniciarViajeRequest;
import pe.epsel.aguaya.ruta.dto.ViajeResponse;
import pe.epsel.aguaya.ruta.entity.Ruta;
import pe.epsel.aguaya.ruta.entity.Viaje;
import pe.epsel.aguaya.ruta.mapper.RutaMapper;
import pe.epsel.aguaya.ruta.repository.ParadaRepository;
import pe.epsel.aguaya.ruta.repository.ViajeRepository;

/**
 * Viajes de la ruta en curso: la cisterna sale cargada del punto de llenado, reparte y vuelve.
 * Las paradas atendidas mientras hay un viaje en reparto quedan asociadas a él.
 */
@Service
@RequiredArgsConstructor
public class ViajeService {

    private final ConductorRutaService conductorRutaService;
    private final ViajeRepository viajeRepository;
    private final ParadaRepository paradaRepository;
    private final PuntoLlenadoRepository puntoLlenadoRepository;
    private final PublicadorTiempoReal publicador;
    private final Clock clock;

    @Transactional
    public ViajeResponse iniciar(Long rutaId, Long conductorId, IniciarViajeRequest r) {
        Ruta ruta = conductorRutaService.rutaDelConductor(rutaId, conductorId);
        ruta.exigirEnCurso();
        PuntoLlenado punto = r.puntoLlenadoId() == null ? ruta.getPuntoLlenado()
                : puntoLlenadoRepository.findById(r.puntoLlenadoId())
                        .orElseThrow(() -> NegocioException.noEncontrado("Punto de llenado", r.puntoLlenadoId()));
        if (!punto.isActivo()) {
            throw NegocioException.conflicto("PUNTO_LLENADO_INACTIVO", "El punto de llenado está inactivo");
        }
        int capacidad = ruta.getCisterna().getCapacidadLitros();
        if (r.volumenCargadoLitros() > capacidad) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "VOLUMEN_EXCEDE_CAPACIDAD",
                    "La cisterna " + ruta.getCisterna().getPlaca() + " admite hasta " + capacidad + " L");
        }

        OffsetDateTime ahora = OffsetDateTime.now(clock);
        Viaje viaje = ruta.iniciarViaje(punto, r.volumenCargadoLitros(), ahora);
        viajeRepository.saveAndFlush(viaje);
        publicador.eventoRuta(new EventoRuta(EventoRuta.Tipo.VIAJE_INICIADO, rutaId, null, viaje.getId(), ahora));
        return RutaMapper.aViaje(viaje, ruta.getParadas());
    }

    @Transactional
    public ViajeResponse finalizar(Long viajeId, Long conductorId) {
        Viaje viaje = viajeRepository.findById(viajeId)
                .filter(v -> v.getRuta().getConductor() != null
                        && v.getRuta().getConductor().getId().equals(conductorId))
                .orElseThrow(() -> NegocioException.noEncontrado("Viaje", viajeId));
        Ruta ruta = viaje.getRuta();
        ruta.exigirEnCurso();

        OffsetDateTime ahora = OffsetDateTime.now(clock);
        viaje.finalizar(ahora);
        publicador.eventoRuta(new EventoRuta(EventoRuta.Tipo.VIAJE_FINALIZADO, ruta.getId(), null, viajeId, ahora));
        return RutaMapper.aViaje(viaje, paradaRepository.findDetalleByRutaId(ruta.getId()));
    }
}
