package pe.epsel.aguaya.usuario.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.epsel.aguaya.usuario.entity.Vecino;

public interface VecinoRepository extends JpaRepository<Vecino, Long> {

    Optional<Vecino> findByTelefono(String telefono);
}
