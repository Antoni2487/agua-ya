package pe.epsel.aguaya.sector.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pe.epsel.aguaya.sector.entity.InstitucionCritica;
import pe.epsel.aguaya.sector.entity.TipoInstitucion;

public interface InstitucionCriticaRepository extends JpaRepository<InstitucionCritica, Long> {

    @EntityGraph(attributePaths = "sector")
    @Query("SELECT i FROM InstitucionCritica i WHERE (:sectorId IS NULL OR i.sector.id = :sectorId) "
            + "AND (:tipo IS NULL OR i.tipo = :tipo) AND (:activo IS NULL OR i.activo = :activo)")
    Page<InstitucionCritica> buscar(Long sectorId, TipoInstitucion tipo, Boolean activo, Pageable pageable);
}
