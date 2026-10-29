package pe.epsel.aguaya.persona.service;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.epsel.aguaya.common.exception.NegocioException;
import pe.epsel.aguaya.persona.dto.VecinoPerfilResponse;
import pe.epsel.aguaya.persona.entity.Vecino;
import pe.epsel.aguaya.persona.repository.VecinoRepository;
import pe.epsel.aguaya.sector.entity.Sector;

@Service
@RequiredArgsConstructor
public class VecinoService {

    private final VecinoRepository vecinoRepository;

    @Transactional(readOnly = true)
    public VecinoPerfilResponse perfil(Long vecinoId) {
        Vecino v = vecinoRepository.findConSectorById(vecinoId)
                .orElseThrow(() -> NegocioException.noEncontrado("Vecino", vecinoId));
        Sector s = v.getSector();
        return new VecinoPerfilResponse(v.getId(), v.getDni(), v.getNombres(), v.getApellidos(), v.getTelefono(),
                v.getDireccionReferencia(), v.getLatitud(), v.getLongitud(),
                s != null ? new VecinoPerfilResponse.Sector(s.getId(), s.getNombre(), s.getDistrito(), s.getTipoEntrega())
                        : null);
    }
}
