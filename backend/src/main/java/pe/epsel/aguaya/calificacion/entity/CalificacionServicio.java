package pe.epsel.aguaya.calificacion.entity;

import java.time.OffsetDateTime;

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

import pe.epsel.aguaya.persona.entity.Vecino;
import pe.epsel.aguaya.ruta.entity.Parada;

/**
 * Calificación del vecino a una entrega atendida (RF08). Una por parada y vecino.
 */
@Entity
@Table(name = "calificacion_servicio")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CalificacionServicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parada_id", nullable = false)
    private Parada parada;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vecino_id", nullable = false)
    private Vecino vecino;

    @Column(nullable = false)
    private short estrellas;

    @Column(length = 500)
    private String comentario;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    public CalificacionServicio(Parada parada, Vecino vecino, int estrellas, String comentario, OffsetDateTime creadoEn) {
        this.parada = parada;
        this.vecino = vecino;
        this.estrellas = (short) estrellas;
        this.comentario = comentario;
        this.creadoEn = creadoEn;
    }
}
