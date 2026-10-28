package pe.epsel.aguaya.monitoreo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import pe.epsel.aguaya.monitoreo.entity.IntentoGeocerca;

public interface IntentoGeocercaRepository extends JpaRepository<IntentoGeocerca, Long> {

    @EntityGraph(attributePaths = "parada")
    List<IntentoGeocerca> findByParadaRutaIdOrderByRegistradoEnAsc(Long rutaId);
}
