package pe.epsel.aguaya.monitoreo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.epsel.aguaya.monitoreo.entity.MonitoreoGps;

public interface MonitoreoGpsRepository extends JpaRepository<MonitoreoGps, Long> {

    Optional<MonitoreoGps> findFirstByRutaIdOrderByRegistradoEnDesc(Long rutaId);

    List<MonitoreoGps> findByRutaIdOrderByRegistradoEnAsc(Long rutaId);
}
