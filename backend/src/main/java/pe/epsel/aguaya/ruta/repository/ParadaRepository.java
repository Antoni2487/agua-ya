package pe.epsel.aguaya.ruta.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pe.epsel.aguaya.ruta.entity.Parada;

public interface ParadaRepository extends JpaRepository<Parada, Long> {

    @Query("SELECT p FROM Parada p LEFT JOIN FETCH p.vecino LEFT JOIN FETCH p.institucion "
            + "LEFT JOIN FETCH p.puntoEntrega WHERE p.ruta.id = :rutaId ORDER BY p.orden")
    List<Parada> findDetalleByRutaId(Long rutaId);

    /** Paradas pendientes por ruta: {rutaId, pendientes}. */
    @Query("SELECT p.ruta.id, COUNT(p) FROM Parada p WHERE p.ruta.id IN :rutaIds "
            + "AND p.estado = pe.epsel.aguaya.ruta.entity.EstadoParada.PENDIENTE GROUP BY p.ruta.id")
    List<Object[]> contarPendientes(Collection<Long> rutaIds);
}
