package pe.epsel.aguaya.calificacion.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import pe.epsel.aguaya.calificacion.dto.CalificacionResponse;
import pe.epsel.aguaya.calificacion.dto.CalificarRequest;
import pe.epsel.aguaya.calificacion.dto.ResumenCalificacionesResponse;
import pe.epsel.aguaya.calificacion.dto.ResumenCalificacionesResponse.PorConductor;
import pe.epsel.aguaya.calificacion.entity.CalificacionServicio;
import pe.epsel.aguaya.calificacion.mapper.CalificacionMapper;
import pe.epsel.aguaya.calificacion.repository.CalificacionRepository;
import pe.epsel.aguaya.common.dto.PaginaResponse;
import pe.epsel.aguaya.common.exception.NegocioException;
import pe.epsel.aguaya.persona.entity.Vecino;
import pe.epsel.aguaya.persona.repository.VecinoRepository;
import pe.epsel.aguaya.ruta.entity.EstadoParada;
import pe.epsel.aguaya.ruta.entity.Parada;
import pe.epsel.aguaya.ruta.repository.ParadaRepository;
import pe.epsel.aguaya.ruta.service.VecinoRutaService;

/**
 * Calificaciones por parada atendida (RF08, regla 7).
 */
@Service
@RequiredArgsConstructor
public class CalificacionService {

    private final CalificacionRepository calificacionRepository;
    private final ParadaRepository paradaRepository;
    private final VecinoRepository vecinoRepository;
    private final Clock clock;

    /**
     * Solo el vecino dueño de la parada (su hogar o el punto de entrega más cercano), solo si está ATENDIDA
     * y una vez. Una parada ajena se reporta como inexistente.
     */
    @Transactional
    public CalificacionResponse calificar(Long vecinoId, CalificarRequest r) {
        Vecino vecino = vecinoRepository.findById(vecinoId)
                .orElseThrow(() -> NegocioException.noEncontrado("Vecino", vecinoId));
        Parada parada = paradaRepository.findById(r.paradaId())
                .filter(p -> esDelVecino(p, vecino))
                .orElseThrow(() -> NegocioException.noEncontrado("Parada", r.paradaId()));
        if (parada.getEstado() != EstadoParada.ATENDIDA) {
            throw NegocioException.conflicto("PARADA_NO_ATENDIDA",
                    "Solo se califica una entrega atendida; la parada está " + parada.getEstado());
        }
        if (calificacionRepository.existsByParadaIdAndVecinoId(parada.getId(), vecinoId)) {
            throw NegocioException.conflicto("CALIFICACION_DUPLICADA", "Ya calificaste esta entrega");
        }
        String comentario = StringUtils.hasText(r.comentario()) ? r.comentario().trim() : null;
        CalificacionServicio calificacion = calificacionRepository.save(
                new CalificacionServicio(parada, vecino, r.estrellas(), comentario, OffsetDateTime.now(clock)));
        return CalificacionMapper.aResponse(calificacion);
    }

    @Transactional(readOnly = true)
    public PaginaResponse<CalificacionResponse> listar(Long sectorId, Pageable pageable) {
        return PaginaResponse.de(calificacionRepository.buscar(sectorId, pageable), CalificacionMapper::aResponse);
    }

    @Transactional(readOnly = true)
    public ResumenCalificacionesResponse resumen(Long sectorId) {
        Map<Integer, Long> distribucion = new LinkedHashMap<>();
        for (int estrellas = 1; estrellas <= 5; estrellas++) {
            distribucion.put(estrellas, 0L);
        }
        for (Object[] fila : calificacionRepository.distribucion(sectorId)) {
            distribucion.put(((Number) fila[0]).intValue(), (Long) fila[1]);
        }
        long total = distribucion.values().stream().mapToLong(Long::longValue).sum();
        long suma = distribucion.entrySet().stream().mapToLong(e -> e.getKey() * e.getValue()).sum();

        List<PorConductor> conductores = calificacionRepository.porConductor(sectorId).stream()
                .map(f -> new PorConductor((Long) f[0], f[1] + " " + f[2], redondear((Double) f[3]), (Long) f[4]))
                .toList();
        return new ResumenCalificacionesResponse(total, total == 0 ? null : redondear((double) suma / total),
                distribucion, conductores);
    }

    private boolean esDelVecino(Parada parada, Vecino vecino) {
        if (vecino.getSector() == null || !parada.getRuta().getSector().getId().equals(vecino.getSector().getId())) {
            return false;
        }
        return VecinoRutaService.paradaDelVecino(paradaRepository.findDetalleByRutaId(parada.getRuta().getId()), vecino)
                .map(p -> p.getId().equals(parada.getId()))
                .orElse(false);
    }

    private static double redondear(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
