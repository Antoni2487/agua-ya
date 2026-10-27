package pe.epsel.aguaya.ruta.service;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.epsel.aguaya.capacidad.service.CalculadoraViajes;
import pe.epsel.aguaya.cisterna.entity.Cisterna;
import pe.epsel.aguaya.cisterna.entity.EstadoVehiculo;
import pe.epsel.aguaya.cisterna.repository.CisternaRepository;
import pe.epsel.aguaya.common.exception.NegocioException;
import pe.epsel.aguaya.common.util.Haversine;
import pe.epsel.aguaya.persona.entity.Conductor;
import pe.epsel.aguaya.persona.entity.EstadoDisponibilidad;
import pe.epsel.aguaya.persona.repository.ConductorRepository;
import pe.epsel.aguaya.ruta.dto.AsignarRutaRequest;
import pe.epsel.aguaya.ruta.dto.RutaDetalleResponse;
import pe.epsel.aguaya.ruta.dto.SugerenciasAsignacionResponse;
import pe.epsel.aguaya.ruta.dto.SugerenciasAsignacionResponse.CisternaSugerida;
import pe.epsel.aguaya.ruta.dto.SugerenciasAsignacionResponse.ConductorSugerido;
import pe.epsel.aguaya.ruta.entity.EstadoRuta;
import pe.epsel.aguaya.ruta.entity.Ruta;
import pe.epsel.aguaya.ruta.repository.RutaRepository;
import pe.epsel.aguaya.sector.entity.Sector;

/**
 * Asignación de cisterna y conductor (RF03, regla 2). El sistema sugiere y valida; el administrador confirma.
 */
@Service
@RequiredArgsConstructor
public class AsignacionRutaService {

    private final RutaService rutaService;
    private final RutaRepository rutaRepository;
    private final CisternaRepository cisternaRepository;
    private final ConductorRepository conductorRepository;

    /**
     * Cisternas: OPERATIVA, autorización vigente en la fecha y libres en ese turno; primero las que necesitan
     * menos viajes (las cisternas no tienen GPS propio). Conductores: habilitados y libres; primero los de ese
     * turno y luego por cercanía de su última posición al centro del sector.
     */
    @Transactional(readOnly = true)
    public SugerenciasAsignacionResponse sugerencias(Long rutaId) {
        Ruta ruta = rutaEditable(rutaId);
        Sector sector = ruta.getSector();
        Set<Long> cisternasOcupadas = rutaRepository.cisternasOcupadas(ruta.getFechaProgramada(), ruta.getTurno(), rutaId);
        Set<Long> conductoresOcupados = rutaRepository.conductoresOcupados(ruta.getFechaProgramada(), ruta.getTurno(), rutaId);

        List<CisternaSugerida> cisternas = cisternaRepository.findByEstadoVehiculo(EstadoVehiculo.OPERATIVA).stream()
                .filter(c -> c.autorizacionVigente(ruta.getFechaProgramada()))
                .filter(c -> !cisternasOcupadas.contains(c.getId()))
                .map(c -> {
                    int viajes = CalculadoraViajes.viajesNecesarios(sector, c.getCapacidadLitros());
                    return new CisternaSugerida(c.getId(), c.getPlaca(), c.getCapacidadLitros(), viajes,
                            viajes > sector.getMaxViajesDiarios());
                })
                .sorted(Comparator.comparingInt(CisternaSugerida::viajesNecesarios)
                        .thenComparing(CisternaSugerida::placa))
                .toList();

        List<ConductorSugerido> conductores = conductorRepository.findHabilitados().stream()
                .filter(c -> !conductoresOcupados.contains(c.getId()))
                .map(c -> new ConductorSugerido(c.getId(), c.getNombres(), c.getApellidos(), c.getTurno(),
                        c.getTurno() == ruta.getTurno(), distanciaKm(c, sector)))
                .sorted(Comparator.comparing((ConductorSugerido c) -> !c.mismoTurno())
                        .thenComparing(ConductorSugerido::distanciaKm, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ConductorSugerido::id))
                .toList();

        return new SugerenciasAsignacionResponse(rutaId, CalculadoraViajes.litrosPorVisita(sector),
                sector.getMaxViajesDiarios(), cisternas, conductores);
    }

    @Transactional
    public RutaDetalleResponse asignar(Long rutaId, AsignarRutaRequest r) {
        Ruta ruta = rutaEditable(rutaId);
        Cisterna cisterna = cisternaRepository.findById(r.cisternaId())
                .orElseThrow(() -> NegocioException.noEncontrado("Cisterna", r.cisternaId()));
        Conductor conductor = conductorRepository.findById(r.conductorId())
                .orElseThrow(() -> NegocioException.noEncontrado("Conductor", r.conductorId()));

        if (cisterna.getEstadoVehiculo() != EstadoVehiculo.OPERATIVA) {
            throw NegocioException.conflicto("CISTERNA_NO_OPERATIVA",
                    "La cisterna " + cisterna.getPlaca() + " está en estado " + cisterna.getEstadoVehiculo());
        }
        if (!cisterna.autorizacionVigente(ruta.getFechaProgramada())) {
            throw NegocioException.conflicto("CISTERNA_AUTORIZACION_VENCIDA",
                    "La autorización sanitaria de " + cisterna.getPlaca() + " no está vigente para esa fecha");
        }
        if (rutaRepository.cisternasOcupadas(ruta.getFechaProgramada(), ruta.getTurno(), rutaId)
                .contains(cisterna.getId())) {
            throw NegocioException.conflicto("CISTERNA_OCUPADA",
                    "La cisterna ya tiene otra ruta en esa fecha y turno");
        }
        if (!conductor.isActivo() || conductor.getEstadoDisponibilidad() == EstadoDisponibilidad.INACTIVO) {
            throw NegocioException.conflicto("CONDUCTOR_INACTIVO", "El conductor está inactivo");
        }
        if (rutaRepository.conductoresOcupados(ruta.getFechaProgramada(), ruta.getTurno(), rutaId)
                .contains(conductor.getId())) {
            throw NegocioException.conflicto("CONDUCTOR_OCUPADO",
                    "El conductor ya tiene otra ruta en esa fecha y turno");
        }

        ruta.setCisterna(cisterna);
        ruta.setConductor(conductor);
        ruta.setViajesPlanificados(r.viajesPlanificados() != null
                ? r.viajesPlanificados()
                : CalculadoraViajes.viajesNecesarios(ruta.getSector(), cisterna.getCapacidadLitros()));
        rutaRepository.flush();
        return rutaService.obtener(rutaId);
    }

    private Ruta rutaEditable(Long rutaId) {
        Ruta ruta = rutaService.buscar(rutaId);
        if (ruta.getEstado() != EstadoRuta.PROGRAMADA) {
            throw NegocioException.conflicto("RUTA_NO_EDITABLE", "La ruta ya inició; no se puede reasignar");
        }
        return ruta;
    }

    private static Double distanciaKm(Conductor c, Sector sector) {
        if (c.getLatActual() == null || c.getLngActual() == null) {
            return null;
        }
        double metros = Haversine.distanciaMetros(c.getLatActual().doubleValue(), c.getLngActual().doubleValue(),
                sector.getLatCentro().doubleValue(), sector.getLngCentro().doubleValue());
        return Math.round(metros / 100.0) / 10.0;
    }
}
