package pe.epsel.aguaya.cisterna.mapper;

import java.time.LocalDate;

import pe.epsel.aguaya.cisterna.dto.CisternaRequest;
import pe.epsel.aguaya.cisterna.dto.CisternaResponse;
import pe.epsel.aguaya.cisterna.entity.Cisterna;

public final class CisternaMapper {

    private CisternaMapper() {
    }

    public static CisternaResponse aResponse(Cisterna c, LocalDate hoy) {
        return new CisternaResponse(c.getId(), c.getPlaca(), c.getCapacidadLitros(), c.getEstadoVehiculo(),
                c.getNroAutorizacionSanitaria(), c.getVenceAutorizacion(), c.autorizacionVigente(hoy));
    }

    public static void aplicar(CisternaRequest r, Cisterna c) {
        c.setPlaca(normalizarPlaca(r.placa()));
        c.setCapacidadLitros(r.capacidadLitros());
        if (r.estadoVehiculo() != null) {
            c.setEstadoVehiculo(r.estadoVehiculo());
        }
        c.setNroAutorizacionSanitaria(r.nroAutorizacionSanitaria());
        c.setVenceAutorizacion(r.venceAutorizacion());
    }

    public static String normalizarPlaca(String placa) {
        return placa.trim().toUpperCase();
    }
}
