package pe.epsel.aguaya.sector.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Establecimiento con prioridad de atención (posta, bomberos, colegio). Sus paradas son CRITICA.
 */
@Entity
@Table(name = "institucion_critica")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InstitucionCritica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TipoInstitucion tipo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sector_id", nullable = false)
    private Sector sector;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal latitud;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal longitud;

    @Column(nullable = false)
    private boolean activo = true;

    public InstitucionCritica(String nombre, TipoInstitucion tipo, Sector sector,
                              BigDecimal latitud, BigDecimal longitud) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.sector = sector;
        this.latitud = latitud;
        this.longitud = longitud;
    }
}
