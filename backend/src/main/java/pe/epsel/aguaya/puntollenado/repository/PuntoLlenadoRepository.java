package pe.epsel.aguaya.puntollenado.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pe.epsel.aguaya.puntollenado.entity.PuntoLlenado;
import pe.epsel.aguaya.puntollenado.entity.TipoPuntoLlenado;

public interface PuntoLlenadoRepository extends JpaRepository<PuntoLlenado, Long> {

    @Query("SELECT p FROM PuntoLlenado p WHERE (:tipo IS NULL OR p.tipo = :tipo) "
            + "AND (:activo IS NULL OR p.activo = :activo)")
    Page<PuntoLlenado> buscar(TipoPuntoLlenado tipo, Boolean activo, Pageable pageable);
}
