package pe.epsel.aguaya.puntollenado.entity;

import java.math.BigDecimal;

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

/**
 * Lugar donde la cisterna carga agua: planta, surtidor o pozo.
 */
@Entity
@Table(name = "punto_llenado")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PuntoLlenado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoPuntoLlenado tipo;

    @Column(length = 255)
    private String direccion;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal latitud;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal longitud;

    @Column(nullable = false)
    private boolean activo = true;

    public PuntoLlenado(String nombre, TipoPuntoLlenado tipo, BigDecimal latitud, BigDecimal longitud) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.latitud = latitud;
        this.longitud = longitud;
    }
}
