package pe.epsel.aguaya.sector.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Lugar fijo donde se detiene la cisterna y los vecinos llenan sus recipientes.
 * Se usa en sectores con tipo de entrega PUNTO_ENTREGA.
 */
@Entity
@Table(name = "punto_entrega")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PuntoEntrega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sector_id", nullable = false)
    private Sector sector;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 255)
    private String referencia;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal latitud;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal longitud;

    @Column(nullable = false)
    private boolean activo = true;

    public PuntoEntrega(Sector sector, String nombre, BigDecimal latitud, BigDecimal longitud) {
        this.sector = sector;
        this.nombre = nombre;
        this.latitud = latitud;
        this.longitud = longitud;
    }
}
