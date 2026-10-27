package pe.epsel.aguaya.monitoreo.entity;

import java.math.BigDecimal;
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

import pe.epsel.aguaya.persona.entity.Conductor;
import pe.epsel.aguaya.ruta.entity.Parada;

/**
 * Intento rechazado de marcar una parada como atendida fuera de la geocerca (regla 4).
 * Latitud, longitud y distancia son nulas si el conductor aún no había enviado posición.
 */
@Entity
@Table(name = "intento_geocerca")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IntentoGeocerca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parada_id", nullable = false)
    private Parada parada;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conductor_id", nullable = false)
    private Conductor conductor;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitud;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitud;

    @Column(name = "distancia_metros", precision = 10, scale = 1)
    private BigDecimal distanciaMetros;

    @Column(name = "registrado_en", nullable = false)
    private OffsetDateTime registradoEn;

    public IntentoGeocerca(Parada parada, Conductor conductor, BigDecimal latitud, BigDecimal longitud,
                           BigDecimal distanciaMetros, OffsetDateTime registradoEn) {
        this.parada = parada;
        this.conductor = conductor;
        this.latitud = latitud;
        this.longitud = longitud;
        this.distanciaMetros = distanciaMetros;
        this.registradoEn = registradoEn;
    }
}
