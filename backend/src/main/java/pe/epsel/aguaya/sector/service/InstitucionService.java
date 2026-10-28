package pe.epsel.aguaya.sector.service;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.epsel.aguaya.common.util.Coordenada;
import pe.epsel.aguaya.common.dto.PaginaResponse;
import pe.epsel.aguaya.common.exception.NegocioException;
import pe.epsel.aguaya.sector.dto.InstitucionRequest;
import pe.epsel.aguaya.sector.dto.InstitucionResponse;
import pe.epsel.aguaya.sector.entity.InstitucionCritica;
import pe.epsel.aguaya.sector.entity.TipoInstitucion;
import pe.epsel.aguaya.sector.mapper.InstitucionMapper;
import pe.epsel.aguaya.sector.repository.InstitucionCriticaRepository;

@Service
@RequiredArgsConstructor
public class InstitucionService {

    private final InstitucionCriticaRepository institucionRepository;
    private final SectorService sectorService;

    @Transactional(readOnly = true)
    public PaginaResponse<InstitucionResponse> listar(Long sectorId, TipoInstitucion tipo, Boolean activo,
                                                      Pageable pageable) {
        return PaginaResponse.de(institucionRepository.buscar(sectorId, tipo, activo, pageable),
                InstitucionMapper::aResponse);
    }

    @Transactional(readOnly = true)
    public InstitucionResponse obtener(Long id) {
        return InstitucionMapper.aResponse(buscar(id));
    }

    @Transactional
    public InstitucionResponse crear(InstitucionRequest r) {
        InstitucionCritica institucion = new InstitucionCritica(r.nombre().trim(), r.tipo(),
                sectorService.buscar(r.sectorId()), Coordenada.normalizar(r.latitud()), Coordenada.normalizar(r.longitud()));
        if (r.activo() != null) {
            institucion.setActivo(r.activo());
        }
        return InstitucionMapper.aResponse(institucionRepository.save(institucion));
    }

    @Transactional
    public InstitucionResponse actualizar(Long id, InstitucionRequest r) {
        InstitucionCritica institucion = buscar(id);
        institucion.setNombre(r.nombre().trim());
        institucion.setTipo(r.tipo());
        institucion.setSector(sectorService.buscar(r.sectorId()));
        institucion.setLatitud(Coordenada.normalizar(r.latitud()));
        institucion.setLongitud(Coordenada.normalizar(r.longitud()));
        if (r.activo() != null) {
            institucion.setActivo(r.activo());
        }
        return InstitucionMapper.aResponse(institucion);
    }

    /** Baja lógica: deja de generar paradas en las rutas nuevas. */
    @Transactional
    public void desactivar(Long id) {
        buscar(id).setActivo(false);
    }

    private InstitucionCritica buscar(Long id) {
        return institucionRepository.findById(id)
                .orElseThrow(() -> NegocioException.noEncontrado("Institución", id));
    }
}
