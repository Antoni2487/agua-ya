package pe.epsel.aguaya.sector.mapper;

import pe.epsel.aguaya.sector.dto.SectorRequest;
import pe.epsel.aguaya.sector.dto.SectorResponse;
import pe.epsel.aguaya.sector.entity.Sector;

public final class SectorMapper {

    /** Dotación mínima diaria por unidad de uso (SUNASS). */
    public static final int DOTACION_POR_DEFECTO = 80;

    private SectorMapper() {
    }

    public static SectorResponse aResponse(Sector s) {
        return new SectorResponse(s.getId(), s.getNombre(), s.getDistrito(), s.getModalidad(), s.isEsZonaAlta(),
                s.getUnidadesUso(), s.getDotacionLitros(), s.getFrecuenciaDias(), s.getMaxViajesDiarios(),
                s.getLatCentro(), s.getLngCentro(), s.isActivo());
    }

    public static Sector nuevo(SectorRequest r) {
        Sector sector = new Sector(r.nombre().trim(), r.distrito().trim(), r.modalidad(), r.unidadesUso(),
                r.maxViajesDiarios(), r.latCentro(), r.lngCentro());
        aplicar(r, sector);
        return sector;
    }

    public static void aplicar(SectorRequest r, Sector s) {
        s.setNombre(r.nombre().trim());
        s.setDistrito(r.distrito().trim());
        s.setModalidad(r.modalidad());
        s.setEsZonaAlta(r.esZonaAlta());
        s.setUnidadesUso(r.unidadesUso());
        s.setDotacionLitros(r.dotacionLitros() != null ? r.dotacionLitros() : DOTACION_POR_DEFECTO);
        s.setFrecuenciaDias(r.frecuenciaDias());
        s.setMaxViajesDiarios(r.maxViajesDiarios());
        s.setLatCentro(r.latCentro());
        s.setLngCentro(r.lngCentro());
        if (r.activo() != null) {
            s.setActivo(r.activo());
        }
    }
}
