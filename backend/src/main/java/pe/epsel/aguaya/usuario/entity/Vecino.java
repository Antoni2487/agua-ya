package pe.epsel.aguaya.usuario.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import pe.epsel.aguaya.sector.entity.Sector;

@Entity
@Table(name = "vecino")
@PrimaryKeyJoinColumn(name = "usuario_id")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vecino extends Usuario {

    @Column(name = "direccion_referencia", length = 255)
    private String direccionReferencia;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal latitud;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal longitud;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sector_id", nullable = false)
    private Sector sector;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private EstadoVecino estado = EstadoVecino.PENDIENTE_VALIDACION;

    public Vecino(String dni, String nombres, String apellidos, String telefono) {
        super(Rol.VECINO, dni, nombres, apellidos, telefono);
    }
}
