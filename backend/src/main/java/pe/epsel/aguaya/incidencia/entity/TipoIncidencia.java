package pe.epsel.aguaya.incidencia.entity;

import java.util.Set;

import pe.epsel.aguaya.persona.entity.Rol;

/**
 * Categorías de incidencia y quién puede reportarlas.
 */
public enum TipoIncidencia {
    AVERIA(Rol.CONDUCTOR),
    ACCESO_BLOQUEADO(Rol.CONDUCTOR),
    AGUA_AGOTADA(Rol.CONDUCTOR),
    NO_LLEGO(Rol.VECINO),
    LLEGO_TARDE(Rol.VECINO),
    YA_PASO(Rol.VECINO),
    VOLUMEN_INSUFICIENTE(Rol.VECINO),
    OTRO(Rol.CONDUCTOR, Rol.VECINO);

    private final Set<Rol> roles;

    TipoIncidencia(Rol... roles) {
        this.roles = Set.of(roles);
    }

    public boolean permitidoPara(Rol rol) {
        return roles.contains(rol);
    }
}
