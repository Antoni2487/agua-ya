package pe.epsel.aguaya.calificacion.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pe.epsel.aguaya.calificacion.entity.CalificacionServicio;

public interface CalificacionRepository extends JpaRepository<CalificacionServicio, Long> {

    boolean existsByParadaIdAndVecinoId(Long paradaId, Long vecinoId);

    Optional<CalificacionServicio> findByParadaIdAndVecinoId(Long paradaId, Long vecinoId);

    @EntityGraph(attributePaths = {"parada.ruta.sector", "parada.ruta.conductor", "parada.ruta.cisterna", "vecino"})
    @Query("SELECT c FROM CalificacionServicio c WHERE (:sectorId IS NULL OR c.parada.ruta.sector.id = :sectorId)")
    Page<CalificacionServicio> buscar(Long sectorId, Pageable pageable);

    /** Cantidad por número de estrellas: {estrellas, cantidad}. */
    @Query("SELECT c.estrellas, COUNT(c) FROM CalificacionServicio c "
            + "WHERE (:sectorId IS NULL OR c.parada.ruta.sector.id = :sectorId) GROUP BY c.estrellas")
    List<Object[]> distribucion(Long sectorId);

    /** Promedio por conductor: {conductorId, nombres, apellidos, promedio, cantidad}. */
    @Query("SELECT co.id, co.nombres, co.apellidos, AVG(c.estrellas), COUNT(c) FROM CalificacionServicio c "
            + "JOIN c.parada.ruta r JOIN r.conductor co "
            + "WHERE (:sectorId IS NULL OR r.sector.id = :sectorId) "
            + "GROUP BY co.id, co.nombres, co.apellidos ORDER BY AVG(c.estrellas) ASC")
    List<Object[]> porConductor(Long sectorId);
}
