package pe.epsel.aguaya.ruta.entity;

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
import pe.epsel.aguaya.puntollenado.entity.PuntoLlenado;

/**
 * Un ciclo carga -> reparto -> retorno de la cisterna dentro de una ruta.
 */
@Entity
@Table(name = "viaje")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Viaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ruta_id", nullable = false)
    private Ruta ruta;

    @Column(name = "nro_viaje", nullable = false)
    private int nroViaje;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "punto_llenado_id", nullable = false)
    private PuntoLlenado puntoLlenado;

    @Column(name = "volumen_cargado_litros")
    private Integer volumenCargadoLitros;

    private OffsetDateTime salida;

    private OffsetDateTime retorno;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoViaje estado;

    /** La cisterna sale cargada del punto de llenado: el viaje empieza EN_REPARTO. */
    Viaje(Ruta ruta, int nroViaje, PuntoLlenado puntoLlenado, int volumenCargadoLitros, OffsetDateTime salida) {
        this.ruta = ruta;
        this.nroViaje = nroViaje;
        this.puntoLlenado = puntoLlenado;
        this.volumenCargadoLitros = volumenCargadoLitros;
        this.salida = salida;
        this.estado = EstadoViaje.EN_REPARTO;
    }

    public boolean abierto() {
        return estado != EstadoViaje.FINALIZADO;
    }

    /** EN_REPARTO -> FINALIZADO cuando la cisterna se vacía o vuelve a cargar. */
    public void finalizar(OffsetDateTime ahora) {
        if (estado != EstadoViaje.EN_REPARTO) {
            throw NegocioException.conflicto("TRANSICION_INVALIDA",
                    "No se puede finalizar un viaje en estado " + estado);
        }
        this.estado = EstadoViaje.FINALIZADO;
        this.retorno = ahora;
    }
}
