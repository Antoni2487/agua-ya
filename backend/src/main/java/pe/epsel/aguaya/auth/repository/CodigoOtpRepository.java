package pe.epsel.aguaya.auth.repository;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import pe.epsel.aguaya.auth.entity.CodigoOtp;
import pe.epsel.aguaya.auth.entity.PropositoOtp;

public interface CodigoOtpRepository extends JpaRepository<CodigoOtp, Long> {

    long countByTelefonoAndCreadoEnAfter(String telefono, OffsetDateTime desde);

    Optional<CodigoOtp> findFirstByTelefonoAndPropositoAndUsadoFalseOrderByCreadoEnDesc(
            String telefono, PropositoOtp proposito);

    /** Invalida los códigos anteriores al emitir uno nuevo: solo vale el último. */
    @Modifying
    @Query("UPDATE CodigoOtp c SET c.usado = true "
            + "WHERE c.telefono = :telefono AND c.proposito = :proposito AND c.usado = false")
    int invalidarVigentes(String telefono, PropositoOtp proposito);
}
