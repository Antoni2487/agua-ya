package pe.epsel.aguaya.ruta.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.epsel.aguaya.ruta.entity.Viaje;

public interface ViajeRepository extends JpaRepository<Viaje, Long> {
}
