package pe.epsel.aguaya.cisterna.entity;

import java.time.LocalDate;

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
@Table(name = "cisterna")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cisterna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String placa;

    @Column(name = "capacidad_litros", nullable = false)
    private int capacidadLitros;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_vehiculo", nullable = false, length = 15)
    private EstadoVehiculo estadoVehiculo = EstadoVehiculo.OPERATIVA;

    @Column(name = "nro_autorizacion_sanitaria", length = 50)
    private String nroAutorizacionSanitaria;

    @Column(name = "vence_autorizacion")
    private LocalDate venceAutorizacion;

    public Cisterna(String placa, int capacidadLitros) {
        this.placa = placa;
        this.capacidadLitros = capacidadLitros;
    }

    /** Autorización sanitaria (DIRESA/GERESA) vigente en la fecha dada. */
    public boolean autorizacionVigente(LocalDate fecha) {
        return venceAutorizacion != null && !venceAutorizacion.isBefore(fecha);
    }
}
