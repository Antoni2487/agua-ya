package pe.epsel.aguaya.ruta.repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import pe.epsel.aguaya.ruta.entity.Parada;

public interface ParadaRepository extends JpaRepository<Parada, Long> {

    @Query("SELECT p FROM Parada p LEFT JOIN FETCH p.vecino LEFT JOIN FETCH p.institucion "
            + "LEFT JOIN FETCH p.puntoEntrega WHERE p.ruta.id = :rutaId ORDER BY p.orden")
    List<Parada> findDetalleByRutaId(Long rutaId);

    /** Paradas que aún esperan el aviso "cisterna cerca": {id, latitud, longitud}. Sin cargar entidades. */
    @Query("SELECT p.id, p.latitud, p.longitud FROM Parada p WHERE p.ruta.id = :rutaId "
            + "AND p.estado = pe.epsel.aguaya.ruta.entity.EstadoParada.PENDIENTE AND p.avisoCercaniaEn IS NULL "
            + "AND p.tipo <> pe.epsel.aguaya.ruta.entity.TipoParada.INSTITUCION")
    List<Object[]> candidatasAvisoCercania(Long rutaId);

    /** Toma la marca del aviso de cercanía; devuelve 0 si otra posición ya la tomó. */
    @Modifying
    @Query("UPDATE Parada p SET p.avisoCercaniaEn = :ahora WHERE p.id = :id AND p.avisoCercaniaEn IS NULL")
    int marcarAvisoCercania(Long id, OffsetDateTime ahora);

    /** Paradas pendientes por ruta: {rutaId, pendientes}. */
    @Query("SELECT p.ruta.id, COUNT(p) FROM Parada p WHERE p.ruta.id IN :rutaIds "
            + "AND p.estado = pe.epsel.aguaya.ruta.entity.EstadoParada.PENDIENTE GROUP BY p.ruta.id")
    List<Object[]> contarPendientes(Collection<Long> rutaIds);
}
