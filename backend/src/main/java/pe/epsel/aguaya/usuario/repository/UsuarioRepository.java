package pe.epsel.aguaya.usuario.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.epsel.aguaya.usuario.entity.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByDni(String dni);

    boolean existsByDni(String dni);

    boolean existsByTelefono(String telefono);
}
