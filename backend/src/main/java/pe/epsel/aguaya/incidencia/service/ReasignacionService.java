package pe.epsel.aguaya.incidencia.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.epsel.aguaya.common.exception.NegocioException;
import pe.epsel.aguaya.common.util.Haversine;
import pe.epsel.aguaya.incidencia.dto.ReasignacionResponse;
import pe.epsel.aguaya.incidencia.dto.ReasignarRequest;
import pe.epsel.aguaya.incidencia.dto.SugerenciasReasignacionResponse;
import pe.epsel.aguaya.incidencia.dto.SugerenciasReasignacionResponse.RutaCandidata;
import pe.epsel.aguaya.incidencia.entity.EstadoIncidencia;
import pe.epsel.aguaya.incidencia.entity.Incidencia;
import pe.epsel.aguaya.incidencia.mapper.IncidenciaMapper;
import pe.epsel.aguaya.incidencia.repository.IncidenciaRepository;
import pe.epsel.aguaya.monitoreo.dto.EventoRuta;
import pe.epsel.aguaya.monitoreo.service.PublicadorTiempoReal;
import pe.epsel.aguaya.notificacion.service.NotificacionService;
import pe.epsel.aguaya.persona.entity.Conductor;
import pe.epsel.aguaya.persona.repository.PersonaRepository;
import pe.epsel.aguaya.ruta.entity.EstadoParada;
import pe.epsel.aguaya.ruta.entity.EstadoRuta;
import pe.epsel.aguaya.ruta.entity.Parada;
import pe.epsel.aguaya.ruta.entity.PrioridadParada;
import pe.epsel.aguaya.ruta.entity.Ruta;
import pe.epsel.aguaya.ruta.repository.ParadaRepository;
import pe.epsel.aguaya.ruta.repository.RutaRepository;
import pe.epsel.aguaya.ruta.service.OrdenadorParadas;

/**
 * Reasignación manual de paradas pendientes (RF07, regla 6). El administrador elige la ruta destino;
 * el sistema solo sugiere. Sin algoritmo automático.
 */
@Service
@RequiredArgsConstructor
public class ReasignacionService {

    /** La ruta origen tuvo un problema en el reparto: se detuvo o sigue en curso con paradas sin atender. */
    private static final Set<EstadoRuta> ORIGEN_REASIGNABLE = EnumSet.of(EstadoRuta.INTERRUMPIDA, EstadoRuta.EN_CURSO);
    private static final Set<EstadoRuta> DESTINO_DISPONIBLE = EnumSet.of(EstadoRuta.PROGRAMADA, EstadoRuta.EN_CURSO);
    private static final int MAX_SUGERENCIAS = 20;

    private final IncidenciaRepository incidenciaRepository;
    private final RutaRepository rutaRepository;
    private final ParadaRepository paradaRepository;
    private final PersonaRepository personaRepository;
    private final NotificacionService notificacionService;
    private final PublicadorTiempoReal publicador;
    private final Clock clock;

    @Transactional(readOnly = true)
    public SugerenciasReasignacionResponse sugerencias(Long incidenciaId) {
        Incidencia incidencia = abierta(incidenciaId);
        Ruta origen = incidencia.getRuta();
        List<Parada> pendientes = pendientes(origen);
        double[] referencia = pendientes.isEmpty()
                ? new double[] {origen.getSector().getLatCentro().doubleValue(), origen.getSector().getLngCentro().doubleValue()}
                : new double[] {pendientes.get(0).getLatitud().doubleValue(), pendientes.get(0).getLongitud().doubleValue()};

        List<Ruta> rutas = rutaRepository.candidatasReasignacion(origen.getSector().getId(), DESTINO_DISPONIBLE,
                LocalDate.now(clock), origen.getId());
        Map<Long, Long> pendientesPorRuta = rutas.isEmpty() ? Map.of()
                : paradaRepository.contarPendientes(rutas.stream().map(Ruta::getId).toList()).stream()
                        .collect(Collectors.toMap(f -> (Long) f[0], f -> (Long) f[1]));

        List<RutaCandidata> candidatas = rutas.stream()
                .map(r -> candidata(r, referencia, pendientesPorRuta.getOrDefault(r.getId(), 0L)))
                .sorted(Comparator.comparing((RutaCandidata c) -> c.distanciaKm() == null)
                        .thenComparing(RutaCandidata::distanciaKm, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(RutaCandidata::fecha)
                        .thenComparing(RutaCandidata::turno))
                .limit(MAX_SUGERENCIAS)
                .toList();
        return new SugerenciasReasignacionResponse(incidenciaId, origen.getId(), pendientes.size(), candidatas);
    }

    /**
     * Las paradas PENDIENTES de la ruta origen se copian en la destino y en el origen quedan REASIGNADA.
     * Si la destino ya tiene pendiente el mismo hogar, institución o punto, no se duplica.
     */
    @Transactional
    public ReasignacionResponse reasignar(Long incidenciaId, Long adminId, ReasignarRequest r) {
        Incidencia incidencia = abierta(incidenciaId);
        Ruta origen = incidencia.getRuta();
        if (!ORIGEN_REASIGNABLE.contains(origen.getEstado())) {
            throw NegocioException.conflicto("RUTA_ORIGEN_NO_REASIGNABLE", "Solo se reasignan paradas de una ruta "
                    + "INTERRUMPIDA o EN_CURSO; la ruta está " + origen.getEstado());
        }
        if (origen.getId().equals(r.rutaDestinoId())) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "MISMA_RUTA", "La ruta destino debe ser otra");
        }
        Ruta destino = rutaRepository.findById(r.rutaDestinoId())
                .orElseThrow(() -> NegocioException.noEncontrado("Ruta", r.rutaDestinoId()));
        if (!destino.getSector().getId().equals(origen.getSector().getId())) {
            throw NegocioException.conflicto("RUTA_DESTINO_OTRO_SECTOR", "La ruta destino debe ser del mismo sector: "
                    + "la próxima atención, el GPS y los avisos del vecino se filtran por sector");
        }
        if (!DESTINO_DISPONIBLE.contains(destino.getEstado()) || destino.getFechaProgramada().isBefore(LocalDate.now(clock))) {
            throw NegocioException.conflicto("RUTA_DESTINO_NO_DISPONIBLE",
                    "La ruta destino debe estar PROGRAMADA o EN_CURSO y ser de hoy en adelante");
        }
        List<Parada> pendientes = pendientes(origen);
        if (pendientes.isEmpty()) {
            throw NegocioException.conflicto("SIN_PARADAS_PENDIENTES", "La ruta origen no tiene paradas pendientes");
        }

        Set<String> yaEnDestino = destino.getParadas().stream()
                .filter(p -> p.getEstado() == EstadoParada.PENDIENTE)
                .map(Parada::clave)
                .collect(Collectors.toSet());
        List<Parada> copias = new ArrayList<>();
        for (Parada p : pendientes) {
            p.marcarReasignada();
            if (!yaEnDestino.contains(p.clave())) {
                copias.add(p.copiaPendiente());
            }
        }
        agregarEnOrden(destino, copias);
        rutaRepository.flush();

        OffsetDateTime ahora = OffsetDateTime.now(clock);
        incidencia.reasignar(destino, personaRepository.getReferenceById(adminId), ahora);
        publicador.eventoRuta(new EventoRuta(EventoRuta.Tipo.PARADAS_REASIGNADAS, destino.getId(), null, ahora));
        notificacionService.paradasReasignadas(destino.getId(), copias.stream().map(Parada::getId).toList());
        return new ReasignacionResponse(IncidenciaMapper.aResponse(incidencia), origen.getId(), destino.getId(),
                copias.size(), pendientes.size() - copias.size());
    }

    /**
     * Las copias CRITICA van después de las CRITICA de la destino y las NORMAL al final, cada grupo encadenado
     * desde el punto de llenado (regla 3). Luego se renumera toda la ruta.
     */
    private static void agregarEnOrden(Ruta destino, List<Parada> copias) {
        double lat = destino.getPuntoLlenado() != null ? destino.getPuntoLlenado().getLatitud().doubleValue()
                : destino.getSector().getLatCentro().doubleValue();
        double lng = destino.getPuntoLlenado() != null ? destino.getPuntoLlenado().getLongitud().doubleValue()
                : destino.getSector().getLngCentro().doubleValue();
        List<Parada> nuevas = OrdenadorParadas.ordenar(copias, lat, lng);

        List<Parada> existentes = new ArrayList<>(destino.getParadas());
        existentes.sort(Comparator.comparingInt(Parada::getOrden));
        int finCriticas = 0;
        for (int i = 0; i < existentes.size(); i++) {
            if (existentes.get(i).getPrioridad() == PrioridadParada.CRITICA) {
                finCriticas = i + 1;
            }
        }
        List<Parada> recorrido = new ArrayList<>(existentes.subList(0, finCriticas));
        nuevas.stream().filter(p -> p.getPrioridad() == PrioridadParada.CRITICA).forEach(recorrido::add);
        recorrido.addAll(existentes.subList(finCriticas, existentes.size()));
        nuevas.stream().filter(p -> p.getPrioridad() != PrioridadParada.CRITICA).forEach(recorrido::add);

        nuevas.forEach(destino::agregarParada);
        for (int i = 0; i < recorrido.size(); i++) {
            recorrido.get(i).setOrden(i + 1);
        }
    }

    private static RutaCandidata candidata(Ruta r, double[] referencia, long pendientes) {
        Conductor c = r.getConductor();
        Double distanciaKm = null;
        if (r.getEstado() == EstadoRuta.EN_CURSO && c != null && c.getLatActual() != null && c.getLngActual() != null) {
            double metros = Haversine.distanciaMetros(c.getLatActual().doubleValue(), c.getLngActual().doubleValue(),
                    referencia[0], referencia[1]);
            distanciaKm = BigDecimal.valueOf(metros / 1000).setScale(2, RoundingMode.HALF_UP).doubleValue();
        }
        return new RutaCandidata(r.getId(), r.getFechaProgramada(), r.getTurno(), r.getEstado(),
                r.getCisterna() != null ? r.getCisterna().getPlaca() : null,
                r.getCisterna() != null ? r.getCisterna().getCapacidadLitros() : null,
                c != null ? c.getNombres() + " " + c.getApellidos() : null,
                distanciaKm, pendientes);
    }

    private List<Parada> pendientes(Ruta ruta) {
        return paradaRepository.findDetalleByRutaId(ruta.getId()).stream()
                .filter(p -> p.getEstado() == EstadoParada.PENDIENTE)
                .toList();
    }

    private Incidencia abierta(Long id) {
        Incidencia incidencia = incidenciaRepository.findById(id)
                .orElseThrow(() -> NegocioException.noEncontrado("Incidencia", id));
        if (incidencia.getEstado() != EstadoIncidencia.ABIERTA) {
            throw NegocioException.conflicto("INCIDENCIA_NO_ABIERTA",
                    "Solo se reasigna una incidencia ABIERTA; está " + incidencia.getEstado());
        }
        return incidencia;
    }
}
