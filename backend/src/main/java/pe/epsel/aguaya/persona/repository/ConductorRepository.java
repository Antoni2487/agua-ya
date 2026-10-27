package pe.epsel.aguaya.persona.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pe.epsel.aguaya.common.dominio.Turno;
import pe.epsel.aguaya.persona.entity.Conductor;
import pe.epsel.aguaya.persona.entity.EstadoDisponibilidad;

public interface ConductorRepository extends JpaRepository<Conductor, Long> {

    @Query("SELECT c FROM Conductor c WHERE (:estado IS NULL OR c.estadoDisponibilidad = :estado) "
            + "AND (:turno IS NULL OR c.turno = :turno) AND (:activo IS NULL OR c.activo = :activo)")
    Page<Conductor> buscar(EstadoDisponibilidad estado, Turno turno, Boolean activo, Pageable pageable);

    /** Activos y no INACTIVO: pueden recibir una ruta. */
    @Query("SELECT c FROM Conductor c WHERE c.activo = true "
            + "AND c.estadoDisponibilidad <> pe.epsel.aguaya.persona.entity.EstadoDisponibilidad.INACTIVO")
    List<Conductor> findHabilitados();

    boolean existsByLicencia(String licencia);

    boolean existsByLicenciaAndIdNot(String licencia, Long id);
}
