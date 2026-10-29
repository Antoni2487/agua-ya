package pe.epsel.aguaya.ruta.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.epsel.aguaya.common.dto.PaginaResponse;
import pe.epsel.aguaya.common.exception.NegocioException;
import pe.epsel.aguaya.persona.repository.AdministradorRepository;
import pe.epsel.aguaya.persona.repository.VecinoRepository;
import pe.epsel.aguaya.puntollenado.entity.PuntoLlenado;
import pe.epsel.aguaya.puntollenado.repository.PuntoLlenadoRepository;
import pe.epsel.aguaya.ruta.dto.ProgramarRutaRequest;
import pe.epsel.aguaya.ruta.dto.RutaDetalleResponse;
import pe.epsel.aguaya.ruta.dto.RutaResumenResponse;
import pe.epsel.aguaya.ruta.entity.EstadoRuta;
import pe.epsel.aguaya.ruta.entity.Parada;
import pe.epsel.aguaya.ruta.entity.Ruta;
import pe.epsel.aguaya.ruta.mapper.RutaMapper;
import pe.epsel.aguaya.ruta.repository.ParadaRepository;
import pe.epsel.aguaya.ruta.repository.RutaRepository;
import pe.epsel.aguaya.sector.entity.Sector;
import pe.epsel.aguaya.sector.entity.TipoEntrega;
import pe.epsel.aguaya.sector.repository.InstitucionCriticaRepository;
import pe.epsel.aguaya.sector.repository.PuntoEntregaRepository;
import pe.epsel.aguaya.sector.service.SectorService;

/**
 * Programación de rutas (RF01): genera las paradas del sector y las ordena (regla 3).
 */
@Service
@RequiredArgsConstructor
public class RutaService {

    private final RutaRepository rutaRepository;
    private final ParadaRepository paradaRepository;
    private final SectorService sectorService;
    private final PuntoLlenadoRepository puntoLlenadoRepository;
    private final AdministradorRepository administradorRepository;
    private final VecinoRepository vecinoRepository;
    private final InstitucionCriticaRepository institucionRepository;
    private final PuntoEntregaRepository puntoEntregaRepository;
    private final Clock clock;

    @Transactional
    public RutaDetalleResponse programar(ProgramarRutaRequest r, Long administradorId) {
        Sector sector = sectorService.buscar(r.sectorId());
        if (!sector.isActivo()) {
            throw NegocioException.conflicto("SECTOR_INACTIVO", "El sector está inactivo");
        }
        PuntoLlenado puntoLlenado = puntoLlenadoRepository.findById(r.puntoLlenadoId())
                .orElseThrow(() -> NegocioException.noEncontrado("Punto de llenado", r.puntoLlenadoId()));
        if (!puntoLlenado.isActivo()) {
            throw NegocioException.conflicto("PUNTO_LLENADO_INACTIVO", "El punto de llenado está inactivo");
        }
        if (r.fecha().isBefore(LocalDate.now(clock))) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "FECHA_PASADA",
                    "No se puede programar una ruta en una fecha pasada");
        }
        if (rutaRepository.existsBySectorIdAndFechaProgramadaAndTurno(sector.getId(), r.fecha(), r.turno())) {
            throw NegocioException.conflicto("RUTA_DUPLICADA",
                    "Ya hay una ruta para ese sector en la misma fecha y turno");
        }

        List<Parada> paradas = generarParadas(sector);
        if (paradas.isEmpty()) {
            throw NegocioException.conflicto("RUTA_SIN_PARADAS",
                    "El sector no tiene vecinos, puntos de entrega ni instituciones activas");
        }

        Ruta ruta = new Ruta(r.fecha(), r.turno(), sector, puntoLlenado,
                administradorRepository.getReferenceById(administradorId));
        OrdenadorParadas.ordenar(paradas, puntoLlenado.getLatitud().doubleValue(),
                puntoLlenado.getLongitud().doubleValue()).forEach(ruta::agregarParada);
        rutaRepository.save(ruta);
        return RutaMapper.aDetalle(ruta, ruta.getParadas());
    }

    @Transactional(readOnly = true)
    public PaginaResponse<RutaResumenResponse> listar(LocalDate fecha, EstadoRuta estado, Long sectorId,
                                                      Pageable pageable) {
        Page<Ruta> pagina = rutaRepository.buscar(fecha, estado, sectorId, pageable);
        Map<Long, long[]> conteos = contarParadas(pagina.getContent().stream().map(Ruta::getId)
                .collect(Collectors.toSet()));
        return PaginaResponse.de(pagina, ruta -> {
            long[] c = conteos.getOrDefault(ruta.getId(), new long[] {0, 0});
            return RutaMapper.aResumen(ruta, c[0], c[1]);
        });
    }

    @Transactional(readOnly = true)
    public RutaDetalleResponse obtener(Long id) {
        Ruta ruta = rutaRepository.findDetalleById(id).orElseThrow(() -> NegocioException.noEncontrado("Ruta", id));
        return RutaMapper.aDetalle(ruta, paradaRepository.findDetalleByRutaId(id));
    }

    /** Solo mientras está PROGRAMADA: el modelo no tiene estado "cancelada". */
    @Transactional
    public void eliminar(Long id) {
        Ruta ruta = buscar(id);
        if (ruta.getEstado() != EstadoRuta.PROGRAMADA) {
            throw NegocioException.conflicto("RUTA_NO_EDITABLE", "Solo se puede eliminar una ruta que no ha iniciado");
        }
        rutaRepository.delete(ruta);
    }

    public Ruta buscar(Long id) {
        return rutaRepository.findById(id).orElseThrow(() -> NegocioException.noEncontrado("Ruta", id));
    }

    /**
     * Regla 3: instituciones críticas activas (CRITICA) + vecinos activos o puntos de entrega activos (NORMAL),
     * según el tipo de entrega del sector.
     */
    private List<Parada> generarParadas(Sector sector) {
        List<Parada> paradas = new ArrayList<>();
        institucionRepository.findBySectorIdAndActivoTrue(sector.getId())
                .forEach(i -> paradas.add(Parada.deInstitucion(i)));
        if (sector.getTipoEntrega() == TipoEntrega.PUNTO_ENTREGA) {
            puntoEntregaRepository.findBySectorIdAndActivoTrue(sector.getId())
                    .forEach(p -> paradas.add(Parada.dePuntoEntrega(p)));
        } else {
            vecinoRepository.findBySectorIdAndActivoTrue(sector.getId())
                    .forEach(v -> paradas.add(Parada.deHogar(v)));
        }
        return paradas;
    }

    private Map<Long, long[]> contarParadas(Set<Long> rutaIds) {
        Map<Long, long[]> conteos = new HashMap<>();
        if (rutaIds.isEmpty()) {
            return conteos;
        }
        for (Object[] fila : rutaRepository.contarParadas(rutaIds)) {
            conteos.put((Long) fila[0], new long[] {((Number) fila[1]).longValue(), ((Number) fila[2]).longValue()});
        }
        return conteos;
    }
}
