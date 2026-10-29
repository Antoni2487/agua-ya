package pe.epsel.aguaya.monitoreo.service;

import java.time.Clock;
import java.time.OffsetDateTime;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.epsel.aguaya.common.util.Coordenada;
import pe.epsel.aguaya.common.exception.NegocioException;
import pe.epsel.aguaya.monitoreo.dto.PosicionRequest;
import pe.epsel.aguaya.monitoreo.dto.PosicionResponse;
import pe.epsel.aguaya.monitoreo.dto.RecorridoResponse;
import pe.epsel.aguaya.monitoreo.entity.MonitoreoGps;
import pe.epsel.aguaya.monitoreo.repository.IntentoGeocercaRepository;
import pe.epsel.aguaya.monitoreo.repository.MonitoreoGpsRepository;
import pe.epsel.aguaya.notificacion.service.NotificacionService;
import pe.epsel.aguaya.persona.entity.Conductor;
import pe.epsel.aguaya.persona.entity.Vecino;
import pe.epsel.aguaya.persona.repository.VecinoRepository;
import pe.epsel.aguaya.ruta.entity.EstadoRuta;
import pe.epsel.aguaya.ruta.entity.Ruta;
import pe.epsel.aguaya.ruta.repository.RutaRepository;
import pe.epsel.aguaya.ruta.service.ConductorRutaService;

/**
 * Monitoreo GPS (RF04): el conductor envía su posición, se guarda, se actualiza su última
 * ubicación y se publica a quienes siguen la ruta.
 */
@Service
@RequiredArgsConstructor
public class MonitoreoService {

    private final MonitoreoGpsRepository monitoreoRepository;
    private final IntentoGeocercaRepository intentoRepository;
    private final RutaRepository rutaRepository;
    private final VecinoRepository vecinoRepository;
    private final ConductorRutaService conductorRutaService;
    private final PublicadorTiempoReal publicador;
    private final NotificacionService notificacionService;
    private final Clock clock;

    @Transactional
    public PosicionResponse registrarPosicion(Long rutaId, Long conductorId, PosicionRequest r) {
        Ruta ruta = conductorRutaService.rutaDelConductor(rutaId, conductorId);
        ruta.exigirEnCurso();

        OffsetDateTime ahora = OffsetDateTime.now(clock);
        var latitud = Coordenada.normalizar(r.latitud());
        var longitud = Coordenada.normalizar(r.longitud());
        monitoreoRepository.save(new MonitoreoGps(ruta, latitud, longitud, ahora));

        Conductor conductor = ruta.getConductor();
        conductor.setLatActual(latitud);
        conductor.setLngActual(longitud);
        conductor.setUbicacionActualizadaEn(ahora);

        PosicionResponse posicion = new PosicionResponse(rutaId, latitud, longitud, ahora);
        publicador.posicion(posicion);
        notificacionService.posicionCisterna(rutaId, latitud.doubleValue(), longitud.doubleValue(), ahora);
        return posicion;
    }

    /**
     * Regla 9: el vecino solo ve la cisterna de una ruta de su sector y mientras está EN_CURSO.
     */
    @Transactional(readOnly = true)
    public PosicionResponse posicionParaVecino(Long rutaId, Long vecinoId) {
        Vecino vecino = vecinoRepository.findById(vecinoId)
                .orElseThrow(() -> NegocioException.noEncontrado("Vecino", vecinoId));
        Ruta ruta = rutaRepository.findById(rutaId)
                .filter(rt -> rt.getSector().getId().equals(vecino.getSector().getId()))
                .orElseThrow(() -> NegocioException.noEncontrado("Ruta", rutaId));
        if (ruta.getEstado() != EstadoRuta.EN_CURSO) {
            throw NegocioException.conflicto("RUTA_NO_EN_CURSO",
                    "La ubicación de la cisterna solo se muestra mientras la ruta está en curso");
        }
        return monitoreoRepository.findFirstByRutaIdOrderByRegistradoEnDesc(rutaId)
                .map(MonitoreoService::aResponse)
                .orElseThrow(() -> NegocioException.noEncontrado("Posición de la ruta", rutaId));
    }

    @Transactional(readOnly = true)
    public RecorridoResponse recorrido(Long rutaId) {
        if (!rutaRepository.existsById(rutaId)) {
            throw NegocioException.noEncontrado("Ruta", rutaId);
        }
        return new RecorridoResponse(rutaId,
                monitoreoRepository.findByRutaIdOrderByRegistradoEnAsc(rutaId).stream()
                        .map(MonitoreoService::aResponse).toList(),
                intentoRepository.findByParadaRutaIdOrderByRegistradoEnAsc(rutaId).stream()
                        .map(i -> new RecorridoResponse.IntentoFallido(i.getParada().getId(),
                                i.getParada().getOrden(), i.getLatitud(), i.getLongitud(),
                                i.getDistanciaMetros(), i.getRegistradoEn()))
                        .toList());
    }

    private static PosicionResponse aResponse(MonitoreoGps m) {
        return new PosicionResponse(m.getRuta().getId(), m.getLatitud(), m.getLongitud(), m.getRegistradoEn());
    }
}
