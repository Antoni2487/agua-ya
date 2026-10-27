package pe.epsel.aguaya.ruta.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

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

import pe.epsel.aguaya.persona.entity.Vecino;
import pe.epsel.aguaya.sector.entity.InstitucionCritica;
import pe.epsel.aguaya.sector.entity.PuntoEntrega;

/**
 * Punto de entrega dentro de una ruta: un hogar, una institución crítica o un punto de entrega del sector.
 * Guarda sus propias coordenadas para que la geocerca no cambie si luego se edita el origen.
 */
@Entity
@Table(name = "parada")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Parada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ruta_id", nullable = false)
    private Ruta ruta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TipoParada tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vecino_id")
    private Vecino vecino;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institucion_id")
    private InstitucionCritica institucion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "punto_entrega_id")
    private PuntoEntrega puntoEntrega;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal latitud;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal longitud;

    @Column(nullable = false)
    private int orden;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PrioridadParada prioridad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoParada estado = EstadoParada.PENDIENTE;

    @Column(name = "volumen_entregado_litros")
    private Integer volumenEntregadoLitros;

    @Column(name = "verificada_gps", nullable = false)
    private boolean verificadaGps;

    @Column(name = "atendida_en")
    private OffsetDateTime atendidaEn;

    private Parada(TipoParada tipo, PrioridadParada prioridad, BigDecimal latitud, BigDecimal longitud) {
        this.tipo = tipo;
        this.prioridad = prioridad;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    public static Parada deHogar(Vecino vecino) {
        Parada p = new Parada(TipoParada.HOGAR, PrioridadParada.NORMAL, vecino.getLatitud(), vecino.getLongitud());
        p.vecino = vecino;
        return p;
    }

    public static Parada deInstitucion(InstitucionCritica institucion) {
        Parada p = new Parada(TipoParada.INSTITUCION, PrioridadParada.CRITICA,
                institucion.getLatitud(), institucion.getLongitud());
        p.institucion = institucion;
        return p;
    }

    public static Parada dePuntoEntrega(PuntoEntrega punto) {
        Parada p = new Parada(TipoParada.PUNTO_ENTREGA, PrioridadParada.NORMAL, punto.getLatitud(), punto.getLongitud());
        p.puntoEntrega = punto;
        return p;
    }
}
