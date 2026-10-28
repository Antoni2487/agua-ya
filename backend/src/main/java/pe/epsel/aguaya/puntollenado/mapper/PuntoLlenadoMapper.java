package pe.epsel.aguaya.puntollenado.mapper;

import pe.epsel.aguaya.common.util.Coordenada;
import pe.epsel.aguaya.puntollenado.dto.PuntoLlenadoRequest;
import pe.epsel.aguaya.puntollenado.dto.PuntoLlenadoResponse;
import pe.epsel.aguaya.puntollenado.entity.PuntoLlenado;

public final class PuntoLlenadoMapper {

    private PuntoLlenadoMapper() {
    }

    public static PuntoLlenadoResponse aResponse(PuntoLlenado p) {
        return new PuntoLlenadoResponse(p.getId(), p.getNombre(), p.getTipo(), p.getDireccion(),
                p.getLatitud(), p.getLongitud(), p.isActivo());
    }

    public static void aplicar(PuntoLlenadoRequest r, PuntoLlenado p) {
        p.setNombre(r.nombre().trim());
        p.setTipo(r.tipo());
        p.setDireccion(r.direccion());
        p.setLatitud(Coordenada.normalizar(r.latitud()));
        p.setLongitud(Coordenada.normalizar(r.longitud()));
        if (r.activo() != null) {
            p.setActivo(r.activo());
        }
    }
}
