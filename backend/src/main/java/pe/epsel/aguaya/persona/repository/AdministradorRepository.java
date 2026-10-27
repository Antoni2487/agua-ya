package pe.epsel.aguaya.persona.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.epsel.aguaya.persona.entity.Administrador;

public interface AdministradorRepository extends JpaRepository<Administrador, Long> {
}
