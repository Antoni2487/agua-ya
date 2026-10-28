package pe.epsel.aguaya.sector.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pe.epsel.aguaya.sector.entity.PuntoEntrega;

public interface PuntoEntregaRepository extends JpaRepository<PuntoEntrega, Long> {

    @EntityGraph(attributePaths = "sector")
    @Query("SELECT p FROM PuntoEntrega p WHERE (:sectorId IS NULL OR p.sector.id = :sectorId) "
            + "AND (:activo IS NULL OR p.activo = :activo)")
    Page<PuntoEntrega> buscar(Long sectorId, Boolean activo, Pageable pageable);

    List<PuntoEntrega> findBySectorIdAndActivoTrue(Long sectorId);
}
