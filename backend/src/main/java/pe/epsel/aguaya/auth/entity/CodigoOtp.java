package pe.epsel.aguaya.auth.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "codigo_otp")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CodigoOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 15)
    private String telefono;

    @Column(name = "codigo_hash", nullable = false, length = 100)
    private String codigoHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PropositoOtp proposito;

    @Column(name = "expira_en", nullable = false)
    private OffsetDateTime expiraEn;

    @Column(nullable = false)
    private int intentos;

    @Column(nullable = false)
    private boolean usado;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    public CodigoOtp(String telefono, String codigoHash, PropositoOtp proposito,
                     OffsetDateTime creadoEn, OffsetDateTime expiraEn) {
        this.telefono = telefono;
        this.codigoHash = codigoHash;
        this.proposito = proposito;
        this.creadoEn = creadoEn;
        this.expiraEn = expiraEn;
    }

    public boolean expirado(OffsetDateTime ahora) {
        return !ahora.isBefore(expiraEn);
    }
}
