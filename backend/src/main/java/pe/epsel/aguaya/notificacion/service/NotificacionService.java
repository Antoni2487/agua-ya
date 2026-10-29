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
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

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
import pe.epsel.aguaya.ruta.repository.RutaRepository;
import pe.epsel.aguaya.ruta.service.VecinoRutaService;

/**
 * Notificaciones push al vecino: ruta iniciada, cisterna cerca, entrega registrada y reasignación.
 * Cada vecino recibe solo lo de su parada: su hogar o el punto de entrega más cercano (regla 9).
 * <p>
 * Todo el trabajo (calcular destinatarios, buscar sus dispositivos y llamar a Expo) corre después del
 * commit en el pool {@link PushConfig#EJECUTOR_PUSH}, con su propia transacción: la petición del conductor
 * no espera. Por eso los métodos públicos reciben ids y no entidades.
 */
@Slf4j
@Service
public class NotificacionService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final VecinoRepository vecinoRepository;
    private final RutaRepository rutaRepository;
    private final ParadaRepository paradaRepository;
    private final DispositivoPushRepository dispositivoRepository;
    private final ProveedorPush proveedor;
    private final Executor ejecutor;
    private final TransactionTemplate transaccion;
    private final AguayaProperties properties;

    public NotificacionService(VecinoRepository vecinoRepository, RutaRepository rutaRepository,
                               ParadaRepository paradaRepository, DispositivoPushRepository dispositivoRepository,
                               ProveedorPush proveedor, @Qualifier(PushConfig.EJECUTOR_PUSH) Executor ejecutor,
                               PlatformTransactionManager transactionManager, AguayaProperties properties) {
        this.vecinoRepository = vecinoRepository;
        this.rutaRepository = rutaRepository;
        this.paradaRepository = paradaRepository;
        this.dispositivoRepository = dispositivoRepository;
        this.proveedor = proveedor;
        this.ejecutor = ejecutor;
        this.transaccion = new TransactionTemplate(transactionManager);
        this.properties = properties;
    }

    /** A los vecinos con una parada pendiente en la ruta. */
    public void rutaIniciada(Long rutaId) {
        enSegundoPlano("ruta iniciada " + rutaId, () -> {
            Ruta ruta = rutaRepository.findDetalleById(rutaId).orElseThrow();
            List<Parada> paradas = paradaRepository.findDetalleByRutaId(rutaId);
            Map<Long, List<Long>> porParada = vecinosPorParada(ruta, paradas);
            List<Long> vecinos = paradas.stream()
                    .filter(p -> p.getEstado() == EstadoParada.PENDIENTE)
                    .flatMap(p -> porParada.getOrDefault(p.getId(), List.of()).stream())
                    .toList();
            return mensajes(vecinos, "La cisterna inició su recorrido",
                    "Hoy (" + turno(ruta.getTurno()) + ") reparte agua en " + ruta.getSector().getNombre()
                            + ". Te avisaremos cuando esté cerca.",
                    Map.of("tipo", "RUTA_INICIADA", "rutaId", rutaId));
        });
    }

    /**
     * Con cada posición GPS: avisa una sola vez por parada cuando la cisterna entra en el radio de
     * cercanía ({@code aguaya.push.radio-cercania-metros}). La marca se toma con un UPDATE condicional,
     * así dos posiciones simultáneas no envían el aviso dos veces.
     */
    public void posicionCisterna(Long rutaId, double latitud, double longitud, OffsetDateTime ahora) {
        enSegundoPlano("cercanía ruta " + rutaId, () -> {
            int radio = properties.push().radioCercaniaMetros();
            List<Long> cercanas = paradaRepository.candidatasAvisoCercania(rutaId).stream()
                    .filter(f -> Haversine.distanciaMetros(latitud, longitud, ((Number) f[1]).doubleValue(),
                            ((Number) f[2]).doubleValue()) <= radio)
                    .map(f -> (Long) f[0])
                    .filter(id -> paradaRepository.marcarAvisoCercania(id, ahora) == 1)
                    .toList();
            if (cercanas.isEmpty()) {
                return List.of();
            }
            Ruta ruta = rutaRepository.findDetalleById(rutaId).orElseThrow();
            List<Parada> paradas = paradaRepository.findDetalleByRutaId(rutaId);
            Map<Long, List<Long>> porParada = vecinosPorParada(ruta, paradas);
            List<MensajePush> mensajes = new ArrayList<>();
            for (Parada parada : paradas) {
                if (cercanas.contains(parada.getId())) {
                    mensajes.addAll(mensajes(porParada.getOrDefault(parada.getId(), List.of()),
                            "La cisterna está cerca",
                            "Está a menos de " + radio + " m de " + lugar(parada) + ". Ten listos tus recipientes.",
                            Map.of("tipo", "CISTERNA_CERCA", "rutaId", rutaId, "paradaId", parada.getId())));
                }
            }
            return mensajes;
        });
    }

    public void paradaAtendida(Long paradaId) {
        enSegundoPlano("parada atendida " + paradaId, () -> {
            Parada parada = paradaRepository.findById(paradaId).orElseThrow();
            Ruta ruta = parada.getRuta();
            Map<Long, List<Long>> porParada = vecinosPorParada(ruta, paradaRepository.findDetalleByRutaId(ruta.getId()));
            return mensajes(porParada.getOrDefault(paradaId, List.of()), "Entrega registrada",
                    "La cisterna registró la entrega en " + lugar(parada)
                            + ". Cuéntanos cómo te fue calificando el servicio.",
                    Map.of("tipo", "PARADA_ATENDIDA", "rutaId", ruta.getId(), "paradaId", paradaId));
        });
    }

    /** RF07: a los vecinos cuyas paradas pasaron a otra ruta, con la nueva fecha y turno. */
    public void paradasReasignadas(Long rutaDestinoId, Collection<Long> paradasNuevas) {
        if (paradasNuevas.isEmpty()) {
            return;
        }
        List<Long> nuevas = List.copyOf(paradasNuevas);
        enSegundoPlano("reasignación a ruta " + rutaDestinoId, () -> {
            Ruta destino = rutaRepository.findDetalleById(rutaDestinoId).orElseThrow();
            Map<Long, List<Long>> porParada = vecinosPorParada(destino,
                    paradaRepository.findDetalleByRutaId(rutaDestinoId));
            List<Long> vecinos = nuevas.stream()
                    .flatMap(id -> porParada.getOrDefault(id, List.of()).stream())
                    .distinct()
                    .toList();
            return mensajes(vecinos, "Tu atención fue reprogramada",
                    "La cisterna pasará el " + destino.getFechaProgramada().format(FECHA) + " ("
                            + turno(destino.getTurno()) + "). Te avisaremos cuando inicie su recorrido.",
                    Map.of("tipo", "PARADA_REASIGNADA", "rutaId", rutaDestinoId));
        });
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

    private List<MensajePush> mensajes(Collection<Long> personaIds, String titulo, String cuerpo,
                                       Map<String, Object> datos) {
        if (personaIds.isEmpty()) {
            return List.of();
        }
        return dispositivoRepository.deActivos(personaIds).stream()
                .map(d -> new MensajePush(d.getExpoToken(), titulo, cuerpo, datos))
                .toList();
    }

    /**
     * Después del commit (no se avisa de cambios que se revierten), en el pool de push: prepara los mensajes
     * en una transacción propia y luego llama al proveedor. Si el pool está lleno se descarta: el push es
     * de mejor esfuerzo y el aviso de cercanía se reintenta con la siguiente posición.
     */
    private void enSegundoPlano(String descripcion, Supplier<List<MensajePush>> preparar) {
        Runnable tarea = () -> {
            try {
                List<MensajePush> mensajes = transaccion.execute(estado -> preparar.get());
                if (mensajes != null && !mensajes.isEmpty()) {
                    Set<String> invalidos = proveedor.enviar(mensajes);
                    if (!invalidos.isEmpty()) {
                        dispositivoRepository.eliminarTokens(invalidos);
                    }
                }
            } catch (RuntimeException e) {
                log.error("No se pudo notificar ({})", descripcion, e);
            }
        };
        Runnable encolar = () -> {
            try {
                ejecutor.execute(tarea);
            } catch (RejectedExecutionException e) {
                log.warn("Pool de push lleno: se descarta la notificación ({})", descripcion);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    encolar.run();
                }
            });
        } else {
            encolar.run();
        }
    }

    private static String lugar(Parada parada) {
        return parada.getTipo() == TipoParada.HOGAR ? "tu vivienda" : "el punto " + parada.getPuntoEntrega().getNombre();
    }

    private static String turno(Turno turno) {
        return turno == Turno.MANANA ? "turno mañana" : "turno tarde";
    }
}
