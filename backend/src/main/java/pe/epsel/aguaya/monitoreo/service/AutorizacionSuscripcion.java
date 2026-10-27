package pe.epsel.aguaya.monitoreo.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.epsel.aguaya.common.seguridad.UsuarioAutenticado;
import pe.epsel.aguaya.persona.entity.Rol;
import pe.epsel.aguaya.persona.repository.VecinoRepository;
import pe.epsel.aguaya.ruta.entity.EstadoRuta;
import pe.epsel.aguaya.ruta.repository.RutaRepository;

/**
 * Quién puede suscribirse a qué tópico STOMP (regla 9):
 * <ul>
 *   <li>{@code /topic/admin/**}: solo ADMIN.</li>
 *   <li>{@code /topic/rutas/{id}/posicion}: ADMIN; el conductor de la ruta; un vecino del sector
 *   de la ruta mientras está EN_CURSO.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AutorizacionSuscripcion {

    private static final Pattern TOPICO_POSICION = Pattern.compile("^/topic/rutas/(\\d+)/posicion$");

    private final RutaRepository rutaRepository;
    private final VecinoRepository vecinoRepository;

    @Transactional(readOnly = true)
    public boolean puedeSuscribirse(UsuarioAutenticado usuario, String destino) {
        if (usuario == null || destino == null) {
            return false;
        }
        if (destino.startsWith("/topic/admin/")) {
            return usuario.rol() == Rol.ADMIN;
        }
        Matcher m = TOPICO_POSICION.matcher(destino);
        if (!m.matches()) {
            return false;
        }
        Long rutaId = Long.valueOf(m.group(1));
        return switch (usuario.rol()) {
            case ADMIN -> rutaRepository.existsById(rutaId);
            case CONDUCTOR -> rutaRepository.findById(rutaId)
                    .map(r -> r.getConductor() != null && r.getConductor().getId().equals(usuario.id()))
                    .orElse(false);
            case VECINO -> rutaRepository.findById(rutaId)
                    .filter(r -> r.getEstado() == EstadoRuta.EN_CURSO)
                    .flatMap(r -> vecinoRepository.findById(usuario.id())
                            .map(v -> v.getSector().getId().equals(r.getSector().getId())))
                    .orElse(false);
        };
    }
}
