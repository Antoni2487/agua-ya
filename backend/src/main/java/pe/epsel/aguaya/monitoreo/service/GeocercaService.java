package pe.epsel.aguaya.monitoreo.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.epsel.aguaya.common.config.AguayaProperties;
import pe.epsel.aguaya.common.exception.NegocioException;
import pe.epsel.aguaya.common.util.Haversine;
import pe.epsel.aguaya.monitoreo.dto.AtenderParadaRequest;
import pe.epsel.aguaya.monitoreo.dto.EventoRuta;
import pe.epsel.aguaya.monitoreo.entity.IntentoGeocerca;
import pe.epsel.aguaya.monitoreo.repository.IntentoGeocercaRepository;
import pe.epsel.aguaya.notificacion.service.NotificacionService;
import pe.epsel.aguaya.persona.entity.Conductor;
import pe.epsel.aguaya.ruta.dto.ParadaResponse;
import pe.epsel.aguaya.ruta.entity.Parada;
import pe.epsel.aguaya.ruta.mapper.RutaMapper;
import pe.epsel.aguaya.ruta.repository.ParadaRepository;

/**
 * Verificación de entrega por geocerca (RF05, regla 4).
 */
@Service
@RequiredArgsConstructor
public class GeocercaService {

    private final ParadaRepository paradaRepository;
    private final IntentoGeocercaRepository intentoRepository;
    private final PublicadorTiempoReal publicador;
    private final NotificacionService notificacionService;
    private final AguayaProperties properties;
    private final Clock clock;

    /**
     * La parada pasa a ATENDIDA solo si la última posición del conductor es reciente (por defecto,
     * de los últimos 2 minutos) y está dentro del radio. Si no, se rechaza con 409 y el intento queda
     * registrado (por eso no se revierte la transacción). La vigencia evita que un conductor que dejó
     * de enviar GPS junto a una parada la marque después desde otro lugar.
     */
    @Transactional(noRollbackFor = NegocioException.class)
    public ParadaResponse atender(Long paradaId, Long conductorId, AtenderParadaRequest r) {
        Parada parada = paradaDelConductor(paradaId, conductorId);
        parada.getRuta().exigirEnCurso();
        if (parada.enEstadoFinal()) {
            throw NegocioException.conflicto("PARADA_NO_PENDIENTE", "La parada ya está en estado " + parada.getEstado());
        }

        Conductor conductor = parada.getRuta().getConductor();
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        int radio = properties.geocerca().radioMetros();

        if (conductor.getLatActual() == null || conductor.getLngActual() == null) {
            intentoRepository.save(new IntentoGeocerca(parada, conductor, null, null, null, ahora));
            throw NegocioException.conflicto("SIN_POSICION_GPS",
                    "Aún no se recibió tu ubicación. Activa el GPS y espera unos segundos");
        }
        double distancia = Haversine.distanciaMetros(conductor.getLatActual().doubleValue(),
                conductor.getLngActual().doubleValue(), parada.getLatitud().doubleValue(),
                parada.getLongitud().doubleValue());
        boolean desactualizada = conductor.getUbicacionActualizadaEn() == null
                || conductor.getUbicacionActualizadaEn().isBefore(
                        ahora.minusSeconds(properties.geocerca().vigenciaPosicionSegundos()));
        if (desactualizada || distancia > radio) {
            intentoRepository.save(new IntentoGeocerca(parada, conductor, conductor.getLatActual(),
                    conductor.getLngActual(), BigDecimal.valueOf(distancia).setScale(1, RoundingMode.HALF_UP), ahora));
        }
        if (desactualizada) {
            throw NegocioException.conflicto("POSICION_DESACTUALIZADA",
                    "Tu última ubicación no es reciente. Verifica que el GPS esté activo y vuelve a intentarlo");
        }
        if (distancia > radio) {
            throw NegocioException.conflicto("FUERA_DE_GEOCERCA", "Estás a " + Math.round(distancia)
                    + " m de la parada; debes estar a " + radio + " m o menos para marcarla como atendida");
        }

        parada.marcarAtendida(ahora, r != null ? r.volumenEntregadoLitros() : null);
        publicador.eventoRuta(new EventoRuta(EventoRuta.Tipo.PARADA_ATENDIDA, parada.getRuta().getId(), paradaId, ahora));
        notificacionService.paradaAtendida(paradaId);
        return RutaMapper.aParada(parada);
    }

    /** No requiere geocerca: el conductor informa que no pudo atenderla (el motivo va como incidencia). */
    @Transactional
    public ParadaResponse noAtendida(Long paradaId, Long conductorId) {
        Parada parada = paradaDelConductor(paradaId, conductorId);
        parada.getRuta().exigirEnCurso();
        parada.marcarNoAtendida();
        publicador.eventoRuta(new EventoRuta(EventoRuta.Tipo.PARADA_NO_ATENDIDA, parada.getRuta().getId(), paradaId,
                OffsetDateTime.now(clock)));
        return RutaMapper.aParada(parada);
    }

    private Parada paradaDelConductor(Long paradaId, Long conductorId) {
        return paradaRepository.findById(paradaId)
                .filter(p -> p.getRuta().getConductor() != null
                        && p.getRuta().getConductor().getId().equals(conductorId))
                .orElseThrow(() -> NegocioException.noEncontrado("Parada", paradaId));
    }
}
