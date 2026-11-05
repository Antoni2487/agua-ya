package pe.epsel.aguaya.notificacion.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import pe.epsel.aguaya.notificacion.entity.SuscripcionWebPush;

public interface SuscripcionWebPushRepository extends JpaRepository<SuscripcionWebPush, Long> {

    Optional<SuscripcionWebPush> findByEndpoint(String endpoint);

    @Query("SELECT s FROM SuscripcionWebPush s WHERE s.persona.id IN :personaIds AND s.persona.activo = true")
    List<SuscripcionWebPush> deActivos(Collection<Long> personaIds);

    @Transactional
    @Modifying
    @Query("DELETE FROM SuscripcionWebPush s WHERE s.endpoint = :endpoint AND s.persona.id = :personaId")
    int eliminarDePersona(String endpoint, Long personaId);

    /** Suscripciones que el servicio de push reporta como vencidas (404/410). */
    @Transactional
    @Modifying
    @Query("DELETE FROM SuscripcionWebPush s WHERE s.endpoint IN :endpoints")
    int eliminarEndpoints(Collection<String> endpoints);
}
