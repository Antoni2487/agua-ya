package pe.epsel.aguaya.sector.service;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.epsel.aguaya.common.util.Coordenada;
import pe.epsel.aguaya.common.dto.PaginaResponse;
import pe.epsel.aguaya.common.exception.NegocioException;
import pe.epsel.aguaya.sector.dto.PuntoEntregaRequest;
import pe.epsel.aguaya.sector.dto.PuntoEntregaResponse;
import pe.epsel.aguaya.sector.entity.PuntoEntrega;
import pe.epsel.aguaya.sector.mapper.PuntoEntregaMapper;
import pe.epsel.aguaya.sector.repository.PuntoEntregaRepository;

@Service
@RequiredArgsConstructor
public class PuntoEntregaService {

    private final PuntoEntregaRepository puntoEntregaRepository;
    private final SectorService sectorService;

    @Transactional(readOnly = true)
    public PaginaResponse<PuntoEntregaResponse> listar(Long sectorId, Boolean activo, Pageable pageable) {
        return PaginaResponse.de(puntoEntregaRepository.buscar(sectorId, activo, pageable),
                PuntoEntregaMapper::aResponse);
    }

    @Transactional(readOnly = true)
    public PuntoEntregaResponse obtener(Long id) {
        return PuntoEntregaMapper.aResponse(buscar(id));
    }

    @Transactional
    public PuntoEntregaResponse crear(PuntoEntregaRequest r) {
        PuntoEntrega punto = new PuntoEntrega(sectorService.buscar(r.sectorId()), r.nombre().trim(),
                Coordenada.normalizar(r.latitud()), Coordenada.normalizar(r.longitud()));
        punto.setReferencia(r.referencia());
        if (r.activo() != null) {
            punto.setActivo(r.activo());
        }
        return PuntoEntregaMapper.aResponse(puntoEntregaRepository.save(punto));
    }

    @Transactional
    public PuntoEntregaResponse actualizar(Long id, PuntoEntregaRequest r) {
        PuntoEntrega punto = buscar(id);
        punto.setSector(sectorService.buscar(r.sectorId()));
        punto.setNombre(r.nombre().trim());
        punto.setReferencia(r.referencia());
        punto.setLatitud(Coordenada.normalizar(r.latitud()));
        punto.setLongitud(Coordenada.normalizar(r.longitud()));
        if (r.activo() != null) {
            punto.setActivo(r.activo());
        }
        return PuntoEntregaMapper.aResponse(punto);
    }

    /** Baja lógica: deja de generar paradas en las rutas nuevas. */
    @Transactional
    public void desactivar(Long id) {
        buscar(id).setActivo(false);
    }

    private PuntoEntrega buscar(Long id) {
        return puntoEntregaRepository.findById(id)
                .orElseThrow(() -> NegocioException.noEncontrado("Punto de entrega", id));
    }
}
