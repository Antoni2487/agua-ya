package pe.epsel.aguaya.puntollenado.service;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.epsel.aguaya.common.dto.PaginaResponse;
import pe.epsel.aguaya.common.exception.NegocioException;
import pe.epsel.aguaya.puntollenado.dto.PuntoLlenadoRequest;
import pe.epsel.aguaya.puntollenado.dto.PuntoLlenadoResponse;
import pe.epsel.aguaya.puntollenado.entity.PuntoLlenado;
import pe.epsel.aguaya.puntollenado.entity.TipoPuntoLlenado;
import pe.epsel.aguaya.puntollenado.mapper.PuntoLlenadoMapper;
import pe.epsel.aguaya.puntollenado.repository.PuntoLlenadoRepository;

@Service
@RequiredArgsConstructor
public class PuntoLlenadoService {

    private final PuntoLlenadoRepository puntoLlenadoRepository;

    @Transactional(readOnly = true)
    public PaginaResponse<PuntoLlenadoResponse> listar(TipoPuntoLlenado tipo, Boolean activo, Pageable pageable) {
        return PaginaResponse.de(puntoLlenadoRepository.buscar(tipo, activo, pageable), PuntoLlenadoMapper::aResponse);
    }

    @Transactional(readOnly = true)
    public PuntoLlenadoResponse obtener(Long id) {
        return PuntoLlenadoMapper.aResponse(buscar(id));
    }

    @Transactional
    public PuntoLlenadoResponse crear(PuntoLlenadoRequest r) {
        PuntoLlenado punto = new PuntoLlenado(r.nombre().trim(), r.tipo(), r.latitud(), r.longitud());
        PuntoLlenadoMapper.aplicar(r, punto);
        return PuntoLlenadoMapper.aResponse(puntoLlenadoRepository.save(punto));
    }

    @Transactional
    public PuntoLlenadoResponse actualizar(Long id, PuntoLlenadoRequest r) {
        PuntoLlenado punto = buscar(id);
        PuntoLlenadoMapper.aplicar(r, punto);
        return PuntoLlenadoMapper.aResponse(punto);
    }

    /** Baja lógica: los viajes ya registrados conservan la referencia. */
    @Transactional
    public void desactivar(Long id) {
        buscar(id).setActivo(false);
    }

    private PuntoLlenado buscar(Long id) {
        return puntoLlenadoRepository.findById(id)
                .orElseThrow(() -> NegocioException.noEncontrado("Punto de llenado", id));
    }
}
