package pe.epsel.aguaya.ruta.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pe.epsel.aguaya.common.dominio.Turno;
import pe.epsel.aguaya.ruta.entity.EstadoRuta;
import pe.epsel.aguaya.ruta.entity.Ruta;

public interface RutaRepository extends JpaRepository<Ruta, Long> {

    @EntityGraph(attributePaths = {"sector", "cisterna", "conductor"})
    @Query("SELECT r FROM Ruta r WHERE (CAST(:fecha AS LocalDate) IS NULL OR r.fechaProgramada = :fecha) "
            + "AND (:estado IS NULL OR r.estado = :estado) AND (:sectorId IS NULL OR r.sector.id = :sectorId)")
    Page<Ruta> buscar(LocalDate fecha, EstadoRuta estado, Long sectorId, Pageable pageable);

    boolean existsBySectorIdAndFechaProgramadaAndTurno(Long sectorId, LocalDate fecha, Turno turno);

    @EntityGraph(attributePaths = {"sector", "cisterna", "conductor", "puntoLlenado"})
    List<Ruta> findByConductorIdAndFechaProgramadaOrderByTurnoAsc(Long conductorId, LocalDate fecha);

    boolean existsByConductorIdAndEstado(Long conductorId, EstadoRuta estado);

    /** Cisternas ya asignadas a otra ruta en esa fecha y turno. */
    @Query("SELECT r.cisterna.id FROM Ruta r WHERE r.cisterna IS NOT NULL AND r.fechaProgramada = :fecha "
            + "AND r.turno = :turno AND r.id <> :excluirRutaId")
    Set<Long> cisternasOcupadas(LocalDate fecha, Turno turno, Long excluirRutaId);

    /** Conductores ya asignados a otra ruta en esa fecha y turno (regla 2). */
    @Query("SELECT r.conductor.id FROM Ruta r WHERE r.conductor IS NOT NULL AND r.fechaProgramada = :fecha "
            + "AND r.turno = :turno AND r.id <> :excluirRutaId")
    Set<Long> conductoresOcupados(LocalDate fecha, Turno turno, Long excluirRutaId);

    /** Total de paradas y atendidas por ruta: {rutaId, total, atendidas}. */
    @Query("SELECT p.ruta.id, COUNT(p), "
            + "SUM(CASE WHEN p.estado = pe.epsel.aguaya.ruta.entity.EstadoParada.ATENDIDA THEN 1 ELSE 0 END) "
            + "FROM Parada p WHERE p.ruta.id IN :rutaIds GROUP BY p.ruta.id")
    List<Object[]> contarParadas(Set<Long> rutaIds);
}
