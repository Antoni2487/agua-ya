package pe.epsel.aguaya.sector.service;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.epsel.aguaya.common.dto.PaginaResponse;
import pe.epsel.aguaya.common.exception.NegocioException;
import pe.epsel.aguaya.sector.dto.SectorRequest;
import pe.epsel.aguaya.sector.dto.SectorResponse;
import pe.epsel.aguaya.sector.entity.Sector;
import pe.epsel.aguaya.sector.mapper.SectorMapper;
import pe.epsel.aguaya.sector.repository.SectorRepository;

@Service
@RequiredArgsConstructor
public class SectorService {

    private final SectorRepository sectorRepository;

    @Transactional(readOnly = true)
    public PaginaResponse<SectorResponse> listar(String distrito, Boolean activo, Pageable pageable) {
        return PaginaResponse.de(sectorRepository.buscar(distrito, activo, pageable), SectorMapper::aResponse);
    }

    @Transactional(readOnly = true)
    public SectorResponse obtener(Long id) {
        return SectorMapper.aResponse(buscar(id));
    }

    @Transactional
    public SectorResponse crear(SectorRequest request) {
        if (sectorRepository.existsByNombreIgnoreCaseAndDistritoIgnoreCase(request.nombre().trim(),
                request.distrito().trim())) {
            throw duplicado();
        }
        return SectorMapper.aResponse(sectorRepository.save(SectorMapper.nuevo(request)));
    }

    @Transactional
    public SectorResponse actualizar(Long id, SectorRequest request) {
        Sector sector = buscar(id);
        if (sectorRepository.existsByNombreIgnoreCaseAndDistritoIgnoreCaseAndIdNot(request.nombre().trim(),
                request.distrito().trim(), id)) {
            throw duplicado();
        }
        SectorMapper.aplicar(request, sector);
        return SectorMapper.aResponse(sector);
    }

    /** Baja lógica: el sector queda inactivo y deja de asignarse a vecinos nuevos. */
    @Transactional
    public void desactivar(Long id) {
        buscar(id).setActivo(false);
    }

    public Sector buscar(Long id) {
        return sectorRepository.findById(id).orElseThrow(() -> NegocioException.noEncontrado("Sector", id));
    }

    private static NegocioException duplicado() {
        return NegocioException.conflicto("SECTOR_DUPLICADO", "Ya existe un sector con ese nombre en el distrito");
    }
}
