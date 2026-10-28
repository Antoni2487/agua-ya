package pe.epsel.aguaya.sector.entity;

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

@Entity
@Table(name = "sector")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Sector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 60)
    private String distrito;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Modalidad modalidad;

    @Column(name = "es_zona_alta", nullable = false)
    private boolean esZonaAlta;

    @Column(name = "unidades_uso", nullable = false)
    private int unidadesUso;

    @Column(name = "dotacion_litros", nullable = false)
    private int dotacionLitros = 80;

    @Column(name = "frecuencia_dias", nullable = false)
    private int frecuenciaDias = 1;

    @Column(name = "max_viajes_diarios", nullable = false)
    private int maxViajesDiarios;

    @Column(name = "lat_centro", nullable = false, precision = 9, scale = 6)
    private BigDecimal latCentro;

    @Column(name = "lng_centro", nullable = false, precision = 9, scale = 6)
    private BigDecimal lngCentro;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_entrega", nullable = false, length = 15)
    private TipoEntrega tipoEntrega = TipoEntrega.DOMICILIO;

    @Column(nullable = false)
    private boolean activo = true;

    public Sector(String nombre, String distrito, Modalidad modalidad, int unidadesUso, int maxViajesDiarios,
                  BigDecimal latCentro, BigDecimal lngCentro) {
        this.nombre = nombre;
        this.distrito = distrito;
        this.modalidad = modalidad;
        this.unidadesUso = unidadesUso;
        this.maxViajesDiarios = maxViajesDiarios;
        this.latCentro = latCentro;
        this.lngCentro = lngCentro;
    }
}
