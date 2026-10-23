package pe.epsel.aguaya.usuario.mapper;

import pe.epsel.aguaya.usuario.dto.UsuarioResumen;
import pe.epsel.aguaya.usuario.entity.Usuario;
import pe.epsel.aguaya.usuario.entity.Vecino;

public final class UsuarioMapper {

    private UsuarioMapper() {
    }

    public static UsuarioResumen aResumen(Usuario usuario) {
        return new UsuarioResumen(
                usuario.getId(),
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getRol(),
                usuario instanceof Vecino vecino ? vecino.getEstado() : null);
    }
}
