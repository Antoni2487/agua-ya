package pe.epsel.aguaya.transparencia.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.epsel.aguaya.cisterna.entity.EstadoVehiculo;
import pe.epsel.aguaya.cisterna.repository.CisternaRepository;
import pe.epsel.aguaya.common.config.AguayaProperties;
import pe.epsel.aguaya.common.config.CacheConfig;
import pe.epsel.aguaya.common.exception.NegocioException;
import pe.epsel.aguaya.incidencia.entity.EstadoIncidencia;
import pe.epsel.aguaya.ruta.entity.EstadoParada;
import pe.epsel.aguaya.ruta.entity.EstadoRuta;
import pe.epsel.aguaya.ruta.entity.Ruta;
import pe.epsel.aguaya.sector.entity.Sector;
import pe.epsel.aguaya.sector.repository.SectorRepository;
import pe.epsel.aguaya.transparencia.dto.CronogramaResponse;
import pe.epsel.aguaya.transparencia.dto.ResumenTransparenciaResponse;
import pe.epsel.aguaya.transparencia.dto.ResumenTransparenciaResponse.Calificacion;
import pe.epsel.aguaya.transparencia.dto.ResumenTransparenciaResponse.Flota;
import pe.epsel.aguaya.transparencia.dto.ResumenTransparenciaResponse.Incidencias;
import pe.epsel.aguaya.transparencia.dto.ResumenTransparenciaResponse.Paradas;
import pe.epsel.aguaya.transparencia.dto.ResumenTransparenciaResponse.Rutas;
import pe.epsel.aguaya.transparencia.dto.SectorTransparenciaResponse;
import pe.epsel.aguaya.transparencia.dto.SectorTransparenciaResponse.ProximaAtencion;
import pe.epsel.aguaya.transparencia.repository.TransparenciaRepository;

/**
 * Panel público de transparencia (RF09). Sin autenticación: solo datos agregados por distrito y sector,
 * nunca nombres, teléfonos ni coordenadas de viviendas (regla 9). Las respuestas se cachean unos minutos
 * ({@code CACHE_SEGUNDOS}) porque el panel lo consulta cualquiera y los agregados no cambian al segundo.
 */
@Service
@RequiredArgsConstructor
public class TransparenciaService {

    /** Periodo por defecto de los indicadores: los últimos 30 días, hoy incluido. */
    static final int DIAS_INDICADORES = 30;
    static final int MAX_DIAS_INDICADORES = 366;
    /** Cronograma por defecto: hoy y los 6 días siguientes. */
    static final int DIAS_CRONOGRAMA = 7;
    static final int MAX_DIAS_CRONOGRAMA = 31;
    /** Hasta dónde se busca la próxima atención de cada sector. */
    static final int DIAS_PROXIMA_ATENCION = 60;

    private static final Set<EstadoRuta> POR_ATENDER = EnumSet.of(EstadoRuta.PROGRAMADA, EstadoRuta.EN_CURSO);

    private final TransparenciaRepository repository;
    private final SectorRepository sectorRepository;
    private final CisternaRepository cisternaRepository;
    private final AguayaProperties properties;
    private final Clock clock;

    @Cacheable(CacheConfig.TRANSPARENCIA_RESUMEN)
    @Transactional(readOnly = true)
    public ResumenTransparenciaResponse resumen(LocalDate desde, LocalDate hasta) {
        Periodo p = indicadores(desde, hasta);
        List<Sector> activos = sectorRepository.findByActivoTrue();

        Map<EstadoRuta, Long> rutas = new EnumMap<>(EstadoRuta.class);
        repository.rutasPorEstado(p.desde(), p.hasta()).forEach(f -> rutas.put((EstadoRuta) f[0], (Long) f[1]));

        Map<Long, Map<EstadoParada, Long>> paradasPorSector = paradasPorSector(p);
        Map<EstadoParada, Long> paradas = new EnumMap<>(EstadoParada.class);
        paradasPorSector.values().forEach(m -> m.forEach((estado, n) -> paradas.merge(estado, n, Long::sum)));

        long calificaciones = 0;
        double sumaEstrellas = 0;
        for (Object[] f : repository.calificacionesPorSector(p.desde(), p.hasta())) {
            long n = (Long) f[2];
            calificaciones += n;
            sumaEstrellas += (Double) f[1] * n;
        }

        ZoneId zona = properties.zonaHoraria();
        Map<EstadoIncidencia, Long> incidencias = new EnumMap<>(EstadoIncidencia.class);
        repository.incidenciasPorEstado(p.desde().atStartOfDay(zona).toOffsetDateTime(),
                        p.hasta().plusDays(1).atStartOfDay(zona).toOffsetDateTime())
                .forEach(f -> incidencias.put((EstadoIncidencia) f[0], (Long) f[1]));
        long reportadas = incidencias.values().stream().mapToLong(Long::longValue).sum();

        LocalDate hoy = LocalDate.now(clock);
        var flota = cisternaRepository.findAll();
        long disponibles = flota.stream()
                .filter(c -> c.getEstadoVehiculo() == EstadoVehiculo.OPERATIVA && c.getVenceAutorizacion() != null
                        && !c.getVenceAutorizacion().isBefore(hoy))
                .count();

        return new ResumenTransparenciaResponse(p.desde(), p.hasta(), activos.size(), paradasPorSector.size(),
                (int) activos.stream().map(Sector::getDistrito).distinct().count(),
                new Rutas(rutas.values().stream().mapToLong(Long::longValue).sum(),
                        rutas.getOrDefault(EstadoRuta.PROGRAMADA, 0L), rutas.getOrDefault(EstadoRuta.EN_CURSO, 0L),
                        rutas.getOrDefault(EstadoRuta.COMPLETADA, 0L), rutas.getOrDefault(EstadoRuta.INTERRUMPIDA, 0L)),
                aParadas(paradas),
                repository.litrosDistribuidos(p.desde(), p.hasta()),
                new Calificacion(calificaciones == 0 ? null : redondear(sumaEstrellas / calificaciones), calificaciones),
                new Incidencias(reportadas, reportadas - incidencias.getOrDefault(EstadoIncidencia.ABIERTA, 0L)),
                new Flota(flota.size(), disponibles));
    }

    @Cacheable(CacheConfig.TRANSPARENCIA_SECTORES)
    @Transactional(readOnly = true)
    public List<SectorTransparenciaResponse> sectores(String distrito, LocalDate desde, LocalDate hasta) {
        Periodo p = indicadores(desde, hasta);
        LocalDate hoy = LocalDate.now(clock);
        List<Sector> sectores = sectorRepository.findByActivoTrue().stream()
                .filter(s -> distrito == null || s.getDistrito().equalsIgnoreCase(distrito))
                .sorted(Comparator.comparing(Sector::getDistrito).thenComparing(Sector::getNombre))
                .toList();

        Map<Long, Map<EstadoParada, Long>> paradas = paradasPorSector(p);
        Map<Long, Double> calificacion = repository.calificacionesPorSector(p.desde(), p.hasta()).stream()
                .collect(Collectors.toMap(f -> (Long) f[0], f -> redondear((Double) f[1])));
        Map<Long, LocalDate> ultima = repository.ultimaAtencionPorSector().stream()
                .collect(Collectors.toMap(f -> (Long) f[0], f -> (LocalDate) f[1]));
        Map<Long, ProximaAtencion> proxima = new HashMap<>();
        repository.rutasEntre(hoy, hoy.plusDays(DIAS_PROXIMA_ATENCION)).stream()
                .filter(r -> POR_ATENDER.contains(r.getEstado()))
                .forEach(r -> proxima.putIfAbsent(r.getSector().getId(),
                        new ProximaAtencion(r.getFechaProgramada(), r.getTurno(), r.getEstado())));

        return sectores.stream().map(s -> {
            Paradas ps = aParadas(paradas.getOrDefault(s.getId(), Map.of()));
            return new SectorTransparenciaResponse(s.getId(), s.getNombre(), s.getDistrito(), s.getModalidad(),
                    s.isEsZonaAlta(), s.getTipoEntrega(), s.getUnidadesUso(), s.getFrecuenciaDias(),
                    s.getLatCentro(), s.getLngCentro(), ultima.get(s.getId()), proxima.get(s.getId()),
                    ps.atendidas(), ps.cumplimientoPorcentaje(), calificacion.get(s.getId()));
        }).toList();
    }

    @Cacheable(CacheConfig.TRANSPARENCIA_CRONOGRAMA)
    @Transactional(readOnly = true)
    public List<CronogramaResponse> cronograma(String distrito, LocalDate desde, LocalDate hasta) {
        LocalDate inicio = desde != null ? desde : LocalDate.now(clock);
        LocalDate fin = hasta != null ? hasta : inicio.plusDays(DIAS_CRONOGRAMA - 1);
        validar(inicio, fin, MAX_DIAS_CRONOGRAMA);

        List<Ruta> rutas = repository.rutasEntre(inicio, fin).stream()
                .filter(r -> r.getSector().isActivo())
                .filter(r -> distrito == null || r.getSector().getDistrito().equalsIgnoreCase(distrito))
                .toList();
        Map<Long, long[]> avance = new HashMap<>();
        if (!rutas.isEmpty()) {
            repository.avancePorRuta(rutas.stream().map(Ruta::getId).toList())
                    .forEach(f -> avance.put((Long) f[0], new long[] {(Long) f[1], (Long) f[2]}));
        }
        return rutas.stream()
                .map(r -> {
                    long[] a = avance.getOrDefault(r.getId(), new long[2]);
                    return new CronogramaResponse(r.getId(), r.getFechaProgramada(), r.getTurno(), r.getEstado(),
                            r.getSector().getId(), r.getSector().getNombre(), r.getSector().getDistrito(),
                            r.getSector().getTipoEntrega(), r.getCisterna() != null ? r.getCisterna().getPlaca() : null,
                            a[0], a[1]);
                })
                .toList();
    }

    private Map<Long, Map<EstadoParada, Long>> paradasPorSector(Periodo p) {
        Map<Long, Map<EstadoParada, Long>> porSector = new LinkedHashMap<>();
        for (Object[] f : repository.paradasCerradasPorSector(p.desde(), p.hasta())) {
            porSector.computeIfAbsent((Long) f[0], k -> new EnumMap<>(EstadoParada.class))
                    .put((EstadoParada) f[1], (Long) f[2]);
        }
        return porSector;
    }

    private static Paradas aParadas(Map<EstadoParada, Long> porEstado) {
        long atendidas = porEstado.getOrDefault(EstadoParada.ATENDIDA, 0L);
        long noAtendidas = porEstado.getOrDefault(EstadoParada.NO_ATENDIDA, 0L);
        long sinAtender = porEstado.getOrDefault(EstadoParada.PENDIENTE, 0L);
        long total = atendidas + noAtendidas + sinAtender;
        return new Paradas(total, atendidas, noAtendidas, sinAtender,
                total == 0 ? null : redondear(atendidas * 100.0 / total));
    }

    private Periodo indicadores(LocalDate desde, LocalDate hasta) {
        LocalDate fin = hasta != null ? hasta : LocalDate.now(clock);
        LocalDate inicio = desde != null ? desde : fin.minusDays(DIAS_INDICADORES - 1);
        validar(inicio, fin, MAX_DIAS_INDICADORES);
        return new Periodo(inicio, fin);
    }

    private static void validar(LocalDate desde, LocalDate hasta, int maxDias) {
        if (desde.isAfter(hasta) || ChronoUnit.DAYS.between(desde, hasta) + 1 > maxDias) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "PERIODO_INVALIDO",
                    "desde debe ser anterior o igual a hasta y el periodo de " + maxDias + " días como máximo");
        }
    }

    private static double redondear(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private record Periodo(LocalDate desde, LocalDate hasta) {
    }
}
