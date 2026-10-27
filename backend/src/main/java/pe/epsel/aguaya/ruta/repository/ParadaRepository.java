package pe.epsel.aguaya.ruta.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pe.epsel.aguaya.ruta.entity.Parada;

public interface ParadaRepository extends JpaRepository<Parada, Long> {

    @Query("SELECT p FROM Parada p LEFT JOIN FETCH p.vecino LEFT JOIN FETCH p.institucion "
            + "LEFT JOIN FETCH p.puntoEntrega WHERE p.ruta.id = :rutaId ORDER BY p.orden")
    List<Parada> findDetalleByRutaId(Long rutaId);
}
