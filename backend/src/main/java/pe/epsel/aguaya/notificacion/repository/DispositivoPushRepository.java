package pe.epsel.aguaya.notificacion.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import pe.epsel.aguaya.notificacion.entity.DispositivoPush;

public interface DispositivoPushRepository extends JpaRepository<DispositivoPush, Long> {

    Optional<DispositivoPush> findByExpoToken(String expoToken);

    @Query("SELECT d FROM DispositivoPush d WHERE d.persona.id IN :personaIds AND d.persona.activo = true")
    List<DispositivoPush> deActivos(Collection<Long> personaIds);

    @Transactional
    @Modifying
    @Query("DELETE FROM DispositivoPush d WHERE d.expoToken = :expoToken AND d.persona.id = :personaId")
    int eliminarDePersona(String expoToken, Long personaId);

    /** Tokens que Expo reporta como desinstalados o inválidos. */
    @Transactional
    @Modifying
    @Query("DELETE FROM DispositivoPush d WHERE d.expoToken IN :tokens")
    int eliminarTokens(Collection<String> tokens);
}
