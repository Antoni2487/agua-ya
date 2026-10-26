package pe.epsel.aguaya.cisterna.service;

import java.time.Clock;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.epsel.aguaya.cisterna.dto.CisternaRequest;
import pe.epsel.aguaya.cisterna.dto.CisternaResponse;
import pe.epsel.aguaya.cisterna.entity.Cisterna;
import pe.epsel.aguaya.cisterna.entity.EstadoVehiculo;
import pe.epsel.aguaya.cisterna.mapper.CisternaMapper;
import pe.epsel.aguaya.cisterna.repository.CisternaRepository;
import pe.epsel.aguaya.common.dto.PaginaResponse;
import pe.epsel.aguaya.common.exception.NegocioException;

@Service
@RequiredArgsConstructor
public class CisternaService {

    private final CisternaRepository cisternaRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PaginaResponse<CisternaResponse> listar(EstadoVehiculo estado, Pageable pageable) {
        LocalDate hoy = LocalDate.now(clock);
        return PaginaResponse.de(cisternaRepository.buscar(estado, pageable), c -> CisternaMapper.aResponse(c, hoy));
    }

    @Transactional(readOnly = true)
    public CisternaResponse obtener(Long id) {
        return CisternaMapper.aResponse(buscar(id), LocalDate.now(clock));
    }

    @Transactional
    public CisternaResponse crear(CisternaRequest r) {
        if (cisternaRepository.existsByPlaca(CisternaMapper.normalizarPlaca(r.placa()))) {
            throw placaDuplicada();
        }
        Cisterna cisterna = new Cisterna(CisternaMapper.normalizarPlaca(r.placa()), r.capacidadLitros());
        CisternaMapper.aplicar(r, cisterna);
        return CisternaMapper.aResponse(cisternaRepository.save(cisterna), LocalDate.now(clock));
    }

    @Transactional
    public CisternaResponse actualizar(Long id, CisternaRequest r) {
        Cisterna cisterna = buscar(id);
        if (cisternaRepository.existsByPlacaAndIdNot(CisternaMapper.normalizarPlaca(r.placa()), id)) {
            throw placaDuplicada();
        }
        CisternaMapper.aplicar(r, cisterna);
        return CisternaMapper.aResponse(cisterna, LocalDate.now(clock));
    }

    /**
     * Elimina la cisterna si nunca se asignó a una ruta; si ya tiene rutas se rechaza
     * para conservar el historial (en ese caso se cambia su estado).
     */
    @Transactional
    public void eliminar(Long id) {
        cisternaRepository.delete(buscar(id));
        try {
            cisternaRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw NegocioException.conflicto("CISTERNA_CON_RUTAS",
                    "La cisterna tiene rutas registradas; cambie su estado en lugar de eliminarla");
        }
    }

    private Cisterna buscar(Long id) {
        return cisternaRepository.findById(id).orElseThrow(() -> NegocioException.noEncontrado("Cisterna", id));
    }

    private static NegocioException placaDuplicada() {
        return NegocioException.conflicto("PLACA_DUPLICADA", "Ya existe una cisterna con esa placa");
    }
}
