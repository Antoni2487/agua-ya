package pe.epsel.aguaya.sector.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pe.epsel.aguaya.sector.entity.Sector;

public interface SectorRepository extends JpaRepository<Sector, Long> {

    List<Sector> findByActivoTrue();

    @Query("SELECT s FROM Sector s WHERE (:distrito IS NULL OR s.distrito = :distrito) "
            + "AND (:activo IS NULL OR s.activo = :activo)")
    Page<Sector> buscar(String distrito, Boolean activo, Pageable pageable);

    boolean existsByNombreIgnoreCaseAndDistritoIgnoreCase(String nombre, String distrito);

    boolean existsByNombreIgnoreCaseAndDistritoIgnoreCaseAndIdNot(String nombre, String distrito, Long id);
}
