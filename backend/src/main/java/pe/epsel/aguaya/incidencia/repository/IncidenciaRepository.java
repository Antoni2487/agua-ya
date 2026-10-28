package pe.epsel.aguaya.incidencia.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pe.epsel.aguaya.incidencia.entity.EstadoIncidencia;
import pe.epsel.aguaya.incidencia.entity.Incidencia;
import pe.epsel.aguaya.incidencia.entity.TipoIncidencia;

public interface IncidenciaRepository extends JpaRepository<Incidencia, Long> {

    @EntityGraph(attributePaths = {"ruta", "ruta.sector", "parada", "reportadoPor", "resueltaPor"})
    @Query("SELECT i FROM Incidencia i WHERE (:estado IS NULL OR i.estado = :estado) "
            + "AND (:tipo IS NULL OR i.tipo = :tipo) AND (:rutaId IS NULL OR i.ruta.id = :rutaId)")
    Page<Incidencia> buscar(EstadoIncidencia estado, TipoIncidencia tipo, Long rutaId, Pageable pageable);

    @EntityGraph(attributePaths = {"ruta", "ruta.sector", "parada", "reportadoPor", "resueltaPor"})
    Page<Incidencia> findByReportadoPorId(Long personaId, Pageable pageable);

    boolean existsByReportadoPorIdAndRutaIdAndTipoAndEstado(Long personaId, Long rutaId, TipoIncidencia tipo,
                                                           EstadoIncidencia estado);
}
