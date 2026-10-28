package pe.epsel.aguaya.ruta.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.epsel.aguaya.common.exception.NegocioException;
import pe.epsel.aguaya.common.util.Haversine;
import pe.epsel.aguaya.persona.entity.Vecino;
import pe.epsel.aguaya.persona.repository.VecinoRepository;
import pe.epsel.aguaya.ruta.dto.AtencionVecinoResponse;
import pe.epsel.aguaya.ruta.dto.AtencionVecinoResponse.Atencion;
import pe.epsel.aguaya.ruta.dto.AtencionVecinoResponse.MiParada;
import pe.epsel.aguaya.ruta.dto.ParadaResponse;
import pe.epsel.aguaya.ruta.entity.EstadoParada;
import pe.epsel.aguaya.ruta.entity.EstadoRuta;
import pe.epsel.aguaya.ruta.entity.Parada;
import pe.epsel.aguaya.ruta.entity.Ruta;
import pe.epsel.aguaya.ruta.entity.TipoParada;
import pe.epsel.aguaya.ruta.mapper.RutaMapper;
import pe.epsel.aguaya.ruta.repository.ParadaRepository;
import pe.epsel.aguaya.ruta.repository.RutaRepository;

/**
 * Información al vecino: cuándo le toca y dónde va su cisterna.
 */
@Service
@RequiredArgsConstructor
public class VecinoRutaService {

    private final VecinoRepository vecinoRepository;
    private final RutaRepository rutaRepository;
    private final ParadaRepository paradaRepository;
    private final Clock clock;

    /** Próxima: la ruta EN_CURSO del sector o, si no hay, la siguiente PROGRAMADA desde hoy. */
    @Transactional(readOnly = true)
    public AtencionVecinoResponse proximaAtencion(Long vecinoId) {
        Vecino vecino = vecinoRepository.findById(vecinoId)
                .orElseThrow(() -> NegocioException.noEncontrado("Vecino", vecinoId));
        Long sectorId = vecino.getSector().getId();

        Optional<Ruta> proxima = rutaRepository.findFirstBySectorIdAndEstadoOrderByFechaProgramadaDesc(sectorId,
                        EstadoRuta.EN_CURSO)
                .or(() -> rutaRepository
                        .findFirstBySectorIdAndEstadoAndFechaProgramadaGreaterThanEqualOrderByFechaProgramadaAscTurnoAsc(
                                sectorId, EstadoRuta.PROGRAMADA, LocalDate.now(clock)));
        Optional<Ruta> ultima = rutaRepository.findFirstBySectorIdAndEstadoInOrderByFechaProgramadaDescIdDesc(sectorId,
                EnumSet.of(EstadoRuta.COMPLETADA, EstadoRuta.INTERRUMPIDA));

        return new AtencionVecinoResponse(proxima.map(r -> atencion(r, vecino)).orElse(null),
                ultima.map(r -> atencion(r, vecino)).orElse(null));
    }

    private Atencion atencion(Ruta ruta, Vecino vecino) {
        List<Parada> paradas = paradaRepository.findDetalleByRutaId(ruta.getId());
        long atendidas = paradas.stream().filter(p -> p.getEstado() == EstadoParada.ATENDIDA).count();
        MiParada miParada = paradaDelVecino(paradas, vecino)
                .map(p -> {
                    ParadaResponse datos = RutaMapper.aParada(p);
                    long pendientesAntes = paradas.stream()
                            .filter(o -> o.getOrden() < p.getOrden() && o.getEstado() == EstadoParada.PENDIENTE)
                            .count();
                    return new MiParada(p.getId(), p.getTipo(), datos.nombre(), datos.referencia(), p.getOrden(),
                            p.getEstado(), pendientesAntes);
                })
                .orElse(null);
        return new Atencion(ruta.getId(), ruta.getFechaProgramada(), ruta.getTurno(), ruta.getEstado(),
                paradas.size(), atendidas, miParada);
    }

    /** Su hogar (reparto a domicilio) o el punto de entrega más cercano a su vivienda. */
    public static Optional<Parada> paradaDelVecino(List<Parada> paradas, Vecino vecino) {
        Optional<Parada> hogar = paradas.stream()
                .filter(p -> p.getTipo() == TipoParada.HOGAR && p.getVecino().getId().equals(vecino.getId()))
                .findFirst();
        if (hogar.isPresent()) {
            return hogar;
        }
        double lat = vecino.getLatitud().doubleValue();
        double lng = vecino.getLongitud().doubleValue();
        return paradas.stream()
                .filter(p -> p.getTipo() == TipoParada.PUNTO_ENTREGA)
                .min(Comparator.comparingDouble(p -> Haversine.distanciaMetros(lat, lng,
                        p.getLatitud().doubleValue(), p.getLongitud().doubleValue())));
    }
}
