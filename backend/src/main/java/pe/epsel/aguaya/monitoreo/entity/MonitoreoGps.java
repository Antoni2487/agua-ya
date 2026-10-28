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

import pe.epsel.aguaya.ruta.entity.Ruta;

/**
 * Posición GPS enviada por el celular del conductor durante una ruta EN_CURSO (cada ~15 s).
 */
@Entity
@Table(name = "monitoreo_gps")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MonitoreoGps {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ruta_id", nullable = false)
    private Ruta ruta;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal latitud;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal longitud;

    @Column(name = "registrado_en", nullable = false)
    private OffsetDateTime registradoEn;

    public MonitoreoGps(Ruta ruta, BigDecimal latitud, BigDecimal longitud, OffsetDateTime registradoEn) {
        this.ruta = ruta;
        this.latitud = latitud;
        this.longitud = longitud;
        this.registradoEn = registradoEn;
    }
}
