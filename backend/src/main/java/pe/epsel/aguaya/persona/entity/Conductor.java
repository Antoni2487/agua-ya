package pe.epsel.aguaya.persona.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import pe.epsel.aguaya.common.dominio.Turno;

@Entity
@Table(name = "conductor")
@PrimaryKeyJoinColumn(name = "persona_id")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Conductor extends Persona {

    @Column(nullable = false, length = 20)
    private String licencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_disponibilidad", nullable = false, length = 15)
    private EstadoDisponibilidad estadoDisponibilidad = EstadoDisponibilidad.DISPONIBLE;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Turno turno;

    @Column(name = "lat_actual", precision = 9, scale = 6)
    private BigDecimal latActual;

    @Column(name = "lng_actual", precision = 9, scale = 6)
    private BigDecimal lngActual;

    @Column(name = "ubicacion_actualizada_en")
    private OffsetDateTime ubicacionActualizadaEn;

    public Conductor(String dni, String nombres, String apellidos, String telefono, String passwordHash, String licencia) {
        super(Rol.CONDUCTOR, dni, nombres, apellidos, telefono, passwordHash);
        this.licencia = licencia;
    }
}
