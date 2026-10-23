package pe.epsel.aguaya.sector.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.epsel.aguaya.sector.entity.Sector;

public interface SectorRepository extends JpaRepository<Sector, Long> {

    List<Sector> findByActivoTrue();
}
