package pe.epsel.aguaya.persona.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.epsel.aguaya.persona.entity.Persona;

public interface PersonaRepository extends JpaRepository<Persona, Long> {

    Optional<Persona> findByDni(String dni);

    boolean existsByDni(String dni);

    boolean existsByTelefono(String telefono);

    boolean existsByTelefonoAndIdNot(String telefono, Long id);
}
