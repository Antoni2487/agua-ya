package pe.epsel.aguaya.notificacion.entity;

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

import pe.epsel.aguaya.persona.entity.Persona;

/**
 * Celular donde la persona recibe notificaciones push. Un token pertenece a una sola persona:
 * si otra inicia sesión en el mismo celular, el token pasa a ella.
 */
@Entity
@Table(name = "dispositivo_push")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DispositivoPush {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "persona_id", nullable = false)
    private Persona persona;

    @Column(name = "expo_token", nullable = false, unique = true)
    private String expoToken;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Plataforma plataforma;

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    public DispositivoPush(String expoToken) {
        this.expoToken = expoToken;
    }

    public void asignar(Persona persona, Plataforma plataforma, OffsetDateTime ahora) {
        this.persona = persona;
        this.plataforma = plataforma;
        this.actualizadoEn = ahora;
    }
}
