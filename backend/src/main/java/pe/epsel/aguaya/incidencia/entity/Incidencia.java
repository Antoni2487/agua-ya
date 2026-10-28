package pe.epsel.aguaya.incidencia.entity;

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

import pe.epsel.aguaya.common.exception.NegocioException;
import pe.epsel.aguaya.persona.entity.Persona;
import pe.epsel.aguaya.ruta.entity.Parada;
import pe.epsel.aguaya.ruta.entity.Ruta;

/**
 * Problema reportado por el conductor o un vecino sobre una ruta (RF06).
 */
@Entity
@Table(name = "incidencia")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Incidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ruta_id", nullable = false)
    private Ruta ruta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parada_id")
    private Parada parada;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reportado_por", nullable = false)
    private Persona reportadoPor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private TipoIncidencia tipo;

    @Column(length = 500)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoIncidencia estado = EstadoIncidencia.ABIERTA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ruta_destino_id")
    private Ruta rutaDestino;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resuelta_por")
    private Persona resueltaPor;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @Column(name = "resuelto_en")
    private OffsetDateTime resueltoEn;

    public Incidencia(Ruta ruta, Parada parada, Persona reportadoPor, TipoIncidencia tipo, String descripcion,
                      OffsetDateTime creadoEn) {
        this.ruta = ruta;
        this.parada = parada;
        this.reportadoPor = reportadoPor;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.creadoEn = creadoEn;
    }

    /** RF07: el administrador movió las paradas pendientes de la ruta a otra. */
    public void reasignar(Ruta destino, Persona admin, OffsetDateTime ahora) {
        if (estado != EstadoIncidencia.ABIERTA) {
            throw NegocioException.conflicto("INCIDENCIA_NO_ABIERTA", "Solo se reasigna una incidencia ABIERTA; está " + estado);
        }
        this.estado = EstadoIncidencia.REASIGNADA;
        this.rutaDestino = destino;
        this.resueltaPor = admin;
        this.resueltoEn = ahora;
    }

    public void cerrar(Persona admin, OffsetDateTime ahora) {
        if (estado == EstadoIncidencia.CERRADA) {
            throw NegocioException.conflicto("INCIDENCIA_CERRADA", "La incidencia ya está cerrada");
        }
        this.estado = EstadoIncidencia.CERRADA;
        this.resueltaPor = admin;
        this.resueltoEn = ahora;
    }
}
