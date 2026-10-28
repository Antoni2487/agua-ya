package pe.epsel.aguaya.transparencia.repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import pe.epsel.aguaya.ruta.entity.Ruta;

/**
 * Consultas agregadas del panel público (RF09). Solo conteos y promedios: nada de datos personales.
 */
public interface TransparenciaRepository extends Repository<Ruta, Long> {

    /** {estado, cantidad} de las rutas del periodo. */
    @Query("SELECT r.estado, COUNT(r) FROM Ruta r WHERE r.fechaProgramada BETWEEN :desde AND :hasta GROUP BY r.estado")
    List<Object[]> rutasPorEstado(LocalDate desde, LocalDate hasta);

    /**
     * {sectorId, estado, cantidad} de las paradas de rutas ya cerradas (COMPLETADA o INTERRUMPIDA) del periodo.
     * Las REASIGNADA se excluyen: cuentan en la ruta que las recibió.
     */
    @Query("SELECT p.ruta.sector.id, p.estado, COUNT(p) FROM Parada p "
            + "WHERE p.ruta.fechaProgramada BETWEEN :desde AND :hasta "
            + "AND p.ruta.estado IN (pe.epsel.aguaya.ruta.entity.EstadoRuta.COMPLETADA, "
            + "pe.epsel.aguaya.ruta.entity.EstadoRuta.INTERRUMPIDA) "
            + "AND p.estado <> pe.epsel.aguaya.ruta.entity.EstadoParada.REASIGNADA "
            + "GROUP BY p.ruta.sector.id, p.estado")
    List<Object[]> paradasCerradasPorSector(LocalDate desde, LocalDate hasta);

    @Query("SELECT COALESCE(SUM(v.volumenCargadoLitros), 0) FROM Viaje v "
            + "WHERE v.ruta.fechaProgramada BETWEEN :desde AND :hasta")
    long litrosDistribuidos(LocalDate desde, LocalDate hasta);

    /** {sectorId, promedio, cantidad} de las calificaciones de entregas del periodo. */
    @Query("SELECT c.parada.ruta.sector.id, AVG(c.estrellas), COUNT(c) FROM CalificacionServicio c "
            + "WHERE c.parada.ruta.fechaProgramada BETWEEN :desde AND :hasta GROUP BY c.parada.ruta.sector.id")
    List<Object[]> calificacionesPorSector(LocalDate desde, LocalDate hasta);

    /** {estado, cantidad} de las incidencias reportadas en el periodo. */
    @Query("SELECT i.estado, COUNT(i) FROM Incidencia i WHERE i.creadoEn >= :desde AND i.creadoEn < :hasta "
            + "GROUP BY i.estado")
    List<Object[]> incidenciasPorEstado(OffsetDateTime desde, OffsetDateTime hasta);

    /** {sectorId, fecha} de la última ruta cerrada de cada sector. */
    @Query("SELECT r.sector.id, MAX(r.fechaProgramada) FROM Ruta r "
            + "WHERE r.estado IN (pe.epsel.aguaya.ruta.entity.EstadoRuta.COMPLETADA, "
            + "pe.epsel.aguaya.ruta.entity.EstadoRuta.INTERRUMPIDA) GROUP BY r.sector.id")
    List<Object[]> ultimaAtencionPorSector();

    /** Rutas del rango de fechas, de la más próxima a la más lejana. */
    @EntityGraph(attributePaths = {"sector", "cisterna"})
    @Query("SELECT r FROM Ruta r WHERE r.fechaProgramada BETWEEN :desde AND :hasta "
            + "ORDER BY r.fechaProgramada, r.turno, r.sector.nombre")
    List<Ruta> rutasEntre(LocalDate desde, LocalDate hasta);

    /** {rutaId, total, atendidas} de paradas (sin las REASIGNADA, que se atienden en otra ruta). */
    @Query("SELECT p.ruta.id, COUNT(p), "
            + "SUM(CASE WHEN p.estado = pe.epsel.aguaya.ruta.entity.EstadoParada.ATENDIDA THEN 1 ELSE 0 END) "
            + "FROM Parada p WHERE p.ruta.id IN :rutaIds "
            + "AND p.estado <> pe.epsel.aguaya.ruta.entity.EstadoParada.REASIGNADA GROUP BY p.ruta.id")
    List<Object[]> avancePorRuta(List<Long> rutaIds);
}
