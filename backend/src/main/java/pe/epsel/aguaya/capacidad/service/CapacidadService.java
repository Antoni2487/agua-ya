package pe.epsel.aguaya.capacidad.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.epsel.aguaya.capacidad.dto.CapacidadResponse;
import pe.epsel.aguaya.capacidad.dto.CapacidadResponse.Flota;
import pe.epsel.aguaya.capacidad.dto.CapacidadResponse.SectorCapacidad;
import pe.epsel.aguaya.cisterna.entity.Cisterna;
import pe.epsel.aguaya.cisterna.entity.EstadoVehiculo;
import pe.epsel.aguaya.cisterna.repository.CisternaRepository;
import pe.epsel.aguaya.common.exception.NegocioException;
import pe.epsel.aguaya.sector.entity.Sector;
import pe.epsel.aguaya.sector.repository.SectorRepository;

/**
 * Planificación de capacidad (RF02, regla 1). Es un cálculo: no se persiste nada.
 * <pre>
 * viajes_necesarios    = ⌈ unidades_uso × dotacion × frecuencia ÷ capacidad ⌉
 * cisternas_necesarias = ⌈ viajes_necesarios ÷ max_viajes_diarios ⌉
 * deficit              = max(0, cisternas_necesarias − cisternas_disponibles)
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class CapacidadService {

    private final SectorRepository sectorRepository;
    private final CisternaRepository cisternaRepository;
    private final Clock clock;

    /**
     * @param sectorId        opcional: sin él, todos los sectores activos comparten la flota
     * @param capacidadLitros opcional: simula con un tamaño de cisterna; por defecto, el promedio de las disponibles
     */
    @Transactional(readOnly = true)
    public CapacidadResponse calcular(LocalDate fecha, Long sectorId, Integer capacidadLitros) {
        LocalDate dia = fecha != null ? fecha : LocalDate.now(clock);
        if (capacidadLitros != null && capacidadLitros <= 0) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "CAPACIDAD_INVALIDA",
                    "La capacidad de la cisterna debe ser positiva");
        }

        List<Sector> sectores = sectorId != null
                ? List.of(sectorRepository.findById(sectorId)
                        .orElseThrow(() -> NegocioException.noEncontrado("Sector", sectorId)))
                : sectorRepository.findByActivoTrue().stream()
                        .sorted(Comparator.comparing(Sector::getDistrito).thenComparing(Sector::getNombre))
                        .toList();

        List<Cisterna> flota = cisternaRepository.findAll();
        List<Cisterna> disponibles = flota.stream().filter(c -> disponible(c, dia)).toList();
        int noOperativas = (int) flota.stream().filter(c -> c.getEstadoVehiculo() != EstadoVehiculo.OPERATIVA).count();
        int capacidad = capacidadLitros != null ? capacidadLitros : capacidadPromedio(disponibles, flota);

        List<SectorCapacidad> porSector = sectores.stream()
                .map(s -> porSector(s, capacidad, disponibles.size()))
                .toList();
        int necesarias = porSector.stream().mapToInt(SectorCapacidad::cisternasNecesarias).sum();
        int deficit = Math.max(0, necesarias - disponibles.size());

        return new CapacidadResponse(dia, capacidad, necesarias, disponibles.size(), deficit, deficit > 0,
                new Flota(flota.size(), disponibles.size(), noOperativas,
                        flota.size() - disponibles.size() - noOperativas),
                porSector);
    }

    /** Regla 2: OPERATIVA y con autorización sanitaria vigente en la fecha. */
    private static boolean disponible(Cisterna c, LocalDate dia) {
        return c.getEstadoVehiculo() == EstadoVehiculo.OPERATIVA
                && c.getVenceAutorizacion() != null && !c.getVenceAutorizacion().isBefore(dia);
    }

    /** Sin cisternas disponibles se usa el promedio de toda la flota para poder dimensionar el déficit. */
    private static int capacidadPromedio(List<Cisterna> disponibles, List<Cisterna> flota) {
        List<Cisterna> base = disponibles.isEmpty() ? flota : disponibles;
        if (base.isEmpty()) {
            throw NegocioException.conflicto("SIN_CAPACIDAD_REFERENCIA",
                    "No hay cisternas registradas; indique capacidadLitros para simular");
        }
        return (int) base.stream().mapToInt(Cisterna::getCapacidadLitros).average().orElseThrow();
    }

    private static SectorCapacidad porSector(Sector s, int capacidad, int disponibles) {
        int viajes = CalculadoraViajes.viajesNecesarios(s, capacidad);
        int cisternas = CalculadoraViajes.cisternasNecesarias(viajes, s.getMaxViajesDiarios());
        return new SectorCapacidad(s.getId(), s.getNombre(), s.getDistrito(), s.isEsZonaAlta(), s.getUnidadesUso(),
                s.getDotacionLitros(), s.getFrecuenciaDias(), CalculadoraViajes.litrosPorVisita(s), viajes,
                s.getMaxViajesDiarios(), cisternas, Math.max(0, cisternas - disponibles));
    }
}
