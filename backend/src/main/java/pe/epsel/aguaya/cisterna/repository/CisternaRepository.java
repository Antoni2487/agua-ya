package pe.epsel.aguaya.cisterna.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pe.epsel.aguaya.cisterna.entity.Cisterna;
import pe.epsel.aguaya.cisterna.entity.EstadoVehiculo;

public interface CisternaRepository extends JpaRepository<Cisterna, Long> {

    @Query("SELECT c FROM Cisterna c WHERE (:estado IS NULL OR c.estadoVehiculo = :estado)")
    Page<Cisterna> buscar(EstadoVehiculo estado, Pageable pageable);

    boolean existsByPlaca(String placa);

    boolean existsByPlacaAndIdNot(String placa, Long id);
}
