package pe.epsel.aguaya.usuario.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "administrador")
@PrimaryKeyJoinColumn(name = "usuario_id")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Administrador extends Usuario {

    @Column(length = 100)
    private String cargo;

    @Column(name = "nivel_acceso", length = 30)
    private String nivelAcceso;

    public Administrador(String dni, String nombres, String apellidos, String telefono) {
        super(Rol.ADMIN, dni, nombres, apellidos, telefono);
    }
}
