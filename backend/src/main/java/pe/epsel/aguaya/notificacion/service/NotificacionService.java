package pe.epsel.aguaya.notificacion.service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import pe.epsel.aguaya.common.config.AguayaProperties;
import pe.epsel.aguaya.common.dominio.Turno;
import pe.epsel.aguaya.common.util.Haversine;
import pe.epsel.aguaya.notificacion.config.PushConfig;
import pe.epsel.aguaya.notificacion.repository.DispositivoPushRepository;
import pe.epsel.aguaya.persona.entity.Vecino;
import pe.epsel.aguaya.persona.repository.VecinoRepository;
import pe.epsel.aguaya.ruta.entity.EstadoParada;
import pe.epsel.aguaya.ruta.entity.Parada;
import pe.epsel.aguaya.ruta.entity.Ruta;
import pe.epsel.aguaya.ruta.entity.TipoParada;
import pe.epsel.aguaya.ruta.repository.ParadaRepository;
import pe.epsel.aguaya.ruta.service.VecinoRutaService;

/**
 * Notificaciones push al vecino: ruta iniciada, cisterna cerca y entrega registrada.
 * Cada vecino recibe solo lo de su parada: su hogar o el punto de entrega más cercano (regla 9).
 * Se envían después del commit y fuera del hilo de la petición.
 */
@Slf4j
@Service
public class NotificacionService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final VecinoRepository vecinoRepository;
    private final ParadaRepository paradaRepository;
    private final DispositivoPushRepository dispositivoRepository;
    private final ProveedorPush proveedor;
    private final Executor ejecutor;
    private final AguayaProperties properties;

    public NotificacionService(VecinoRepository vecinoRepository, ParadaRepository paradaRepository,
                               DispositivoPushRepository dispositivoRepository, ProveedorPush proveedor,
                               @Qualifier(PushConfig.EJECUTOR_PUSH) Executor ejecutor, AguayaProperties properties) {
        this.vecinoRepository = vecinoRepository;
        this.paradaRepository = paradaRepository;
        this.dispositivoRepository = dispositivoRepository;
        this.proveedor = proveedor;
        this.ejecutor = ejecutor;
        this.properties = properties;
    }

    /** A los vecinos con una parada pendiente en la ruta. */
    public void rutaIniciada(Ruta ruta) {
        List<Parada> paradas = paradaRepository.findDetalleByRutaId(ruta.getId());
        Map<Long, List<Long>> porParada = vecinosPorParada(ruta, paradas);
        List<Long> vecinos = paradas.stream()
                .filter(p -> p.getEstado() == EstadoParada.PENDIENTE)
                .flatMap(p -> porParada.getOrDefault(p.getId(), List.of()).stream())
                .toList();
        enviar(vecinos, "La cisterna inició su recorrido",
                "Hoy (" + turno(ruta.getTurno()) + ") reparte agua en " + ruta.getSector().getNombre()
                        + ". Te avisaremos cuando esté cerca.",
                Map.of("tipo", "RUTA_INICIADA", "rutaId", ruta.getId()));
    }

    /**
     * Con cada posición GPS: avisa una sola vez por parada cuando la cisterna entra en el radio de
     * cercanía ({@code aguaya.push.radio-cercania-metros}).
     */
    public void posicionCisterna(Ruta ruta, double latitud, double longitud, OffsetDateTime ahora) {
        int radio = properties.push().radioCercaniaMetros();
        List<Parada> cercanas = ruta.getParadas().stream()
                .filter(p -> p.getTipo() != TipoParada.INSTITUCION && p.getEstado() == EstadoParada.PENDIENTE
                        && p.getAvisoCercaniaEn() == null)
                .filter(p -> Haversine.distanciaMetros(latitud, longitud, p.getLatitud().doubleValue(),
                        p.getLongitud().doubleValue()) <= radio)
                .toList();
        if (cercanas.isEmpty()) {
            return;
        }
        List<Parada> paradas = paradaRepository.findDetalleByRutaId(ruta.getId());
        Map<Long, List<Long>> porParada = vecinosPorParada(ruta, paradas);
        for (Parada parada : cercanas) {
            parada.setAvisoCercaniaEn(ahora);
            enviar(porParada.getOrDefault(parada.getId(), List.of()), "La cisterna está cerca",
                    "Está a menos de " + radio + " m de " + lugar(parada) + ". Ten listos tus recipientes.",
                    Map.of("tipo", "CISTERNA_CERCA", "rutaId", ruta.getId(), "paradaId", parada.getId()));
        }
    }

    public void paradaAtendida(Parada parada) {
        Ruta ruta = parada.getRuta();
        Map<Long, List<Long>> porParada = vecinosPorParada(ruta, paradaRepository.findDetalleByRutaId(ruta.getId()));
        enviar(porParada.getOrDefault(parada.getId(), List.of()), "Entrega registrada",
                "La cisterna registró la entrega en " + lugar(parada) + ". Cuéntanos cómo te fue calificando el servicio.",
                Map.of("tipo", "PARADA_ATENDIDA", "rutaId", ruta.getId(), "paradaId", parada.getId()));
    }

    /** RF07: a los vecinos cuyas paradas pasaron a otra ruta, con la nueva fecha y turno. */
    public void paradasReasignadas(Ruta destino, List<Parada> nuevas) {
        if (nuevas.isEmpty()) {
            return;
        }
        Map<Long, List<Long>> porParada = vecinosPorParada(destino, paradaRepository.findDetalleByRutaId(destino.getId()));
        List<Long> vecinos = nuevas.stream()
                .flatMap(p -> porParada.getOrDefault(p.getId(), List.of()).stream())
                .distinct()
                .toList();
        enviar(vecinos, "Tu atención fue reprogramada",
                "La cisterna pasará el " + destino.getFechaProgramada().format(FECHA) + " (" + turno(destino.getTurno())
                        + "). Te avisaremos cuando inicie su recorrido.",
                Map.of("tipo", "PARADA_REASIGNADA", "rutaId", destino.getId()));
    }

    /** Vecinos activos del sector agrupados por su parada en la ruta (las instituciones no tienen vecinos). */
    private Map<Long, List<Long>> vecinosPorParada(Ruta ruta, List<Parada> paradas) {
        Map<Long, List<Long>> porParada = new HashMap<>();
        for (Vecino vecino : vecinoRepository.findBySectorIdAndActivoTrue(ruta.getSector().getId())) {
            VecinoRutaService.paradaDelVecino(paradas, vecino).ifPresent(p ->
                    porParada.computeIfAbsent(p.getId(), k -> new ArrayList<>()).add(vecino.getId()));
        }
        return porParada;
    }

    private void enviar(Collection<Long> personaIds, String titulo, String cuerpo, Map<String, Object> datos) {
        if (personaIds.isEmpty()) {
            return;
        }
        List<MensajePush> mensajes = dispositivoRepository.deActivos(personaIds).stream()
                .map(d -> new MensajePush(d.getExpoToken(), titulo, cuerpo, datos))
                .toList();
        if (mensajes.isEmpty()) {
            return;
        }
        Runnable envio = () -> ejecutor.execute(() -> {
            try {
                Set<String> invalidos = proveedor.enviar(mensajes);
                if (!invalidos.isEmpty()) {
                    dispositivoRepository.eliminarTokens(invalidos);
                }
            } catch (RuntimeException e) {
                log.error("No se pudieron enviar {} notificaciones push", mensajes.size(), e);
            }
        });
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    envio.run();
                }
            });
        } else {
            envio.run();
        }
    }

    private static String lugar(Parada parada) {
        return parada.getTipo() == TipoParada.HOGAR ? "tu vivienda" : "el punto " + parada.getPuntoEntrega().getNombre();
    }

    private static String turno(Turno turno) {
        return turno == Turno.MANANA ? "turno mañana" : "turno tarde";
    }
}
