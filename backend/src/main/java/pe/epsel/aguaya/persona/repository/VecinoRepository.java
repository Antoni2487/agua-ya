package pe.epsel.aguaya.persona.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.epsel.aguaya.persona.entity.Vecino;

public interface VecinoRepository extends JpaRepository<Vecino, Long> {

    Optional<Vecino> findByTelefono(String telefono);

    List<Vecino> findBySectorIdAndActivoTrue(Long sectorId);
}
