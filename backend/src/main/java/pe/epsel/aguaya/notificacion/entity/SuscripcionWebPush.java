package pe.epsel.aguaya.notificacion.entity;

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

import pe.epsel.aguaya.persona.entity.Persona;

/**
 * Suscripción Web Push de un navegador. Un endpoint pertenece a una sola persona: si otra inicia sesión en el
 * mismo navegador, la suscripción pasa a ella.
 */
@Entity
@Table(name = "suscripcion_web_push")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SuscripcionWebPush {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "persona_id", nullable = false)
    private Persona persona;

    @Column(nullable = false, length = 1000, unique = true)
    private String endpoint;

    @Column(nullable = false, length = 100)
    private String p256dh;

    @Column(nullable = false, length = 50)
    private String auth;

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    public SuscripcionWebPush(String endpoint) {
        this.endpoint = endpoint;
    }

    /** Las claves cambian si el navegador renueva la suscripción. */
    public void asignar(Persona persona, String p256dh, String auth, OffsetDateTime ahora) {
        this.persona = persona;
        this.p256dh = p256dh;
        this.auth = auth;
        this.actualizadoEn = ahora;
    }
}
