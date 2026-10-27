package pe.epsel.aguaya.ruta.entity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.CreationTimestamp;

import pe.epsel.aguaya.cisterna.entity.Cisterna;
import pe.epsel.aguaya.common.dominio.Turno;
import pe.epsel.aguaya.persona.entity.Administrador;
import pe.epsel.aguaya.persona.entity.Conductor;
import pe.epsel.aguaya.puntollenado.entity.PuntoLlenado;
import pe.epsel.aguaya.sector.entity.Sector;

/**
 * Reparto programado para un sector, fecha y turno. Cisterna y conductor se asignan después (RF03).
 */
@Entity
@Table(name = "ruta")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ruta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_programada", nullable = false)
    private LocalDate fechaProgramada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Turno turno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sector_id", nullable = false)
    private Sector sector;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "punto_llenado_id")
    private PuntoLlenado puntoLlenado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conductor_id")
    private Conductor conductor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cisterna_id")
    private Cisterna cisterna;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "administrador_id", nullable = false)
    private Administrador administrador;

    @Column(name = "viajes_planificados")
    private Integer viajesPlanificados;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoRuta estado = EstadoRuta.PROGRAMADA;

    private OffsetDateTime inicio;

    private OffsetDateTime fin;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @OneToMany(mappedBy = "ruta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<Parada> paradas = new ArrayList<>();

    public Ruta(LocalDate fechaProgramada, Turno turno, Sector sector, PuntoLlenado puntoLlenado,
                Administrador administrador) {
        this.fechaProgramada = fechaProgramada;
        this.turno = turno;
        this.sector = sector;
        this.puntoLlenado = puntoLlenado;
        this.administrador = administrador;
    }

    public void agregarParada(Parada parada) {
        parada.setRuta(this);
        paradas.add(parada);
    }
}
