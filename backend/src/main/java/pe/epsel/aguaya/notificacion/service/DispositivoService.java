package pe.epsel.aguaya.notificacion.service;

import java.time.Clock;
import java.time.OffsetDateTime;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.epsel.aguaya.notificacion.dto.RegistrarDispositivoRequest;
import pe.epsel.aguaya.notificacion.entity.DispositivoPush;
import pe.epsel.aguaya.notificacion.repository.DispositivoPushRepository;
import pe.epsel.aguaya.persona.repository.PersonaRepository;

@Service
@RequiredArgsConstructor
public class DispositivoService {

    private final DispositivoPushRepository dispositivoRepository;
    private final PersonaRepository personaRepository;
    private final Clock clock;

    /** Idempotente: si el token ya existe se actualiza y, si era de otra persona, pasa a quien lo registra. */
    @Transactional
    public void registrar(Long personaId, RegistrarDispositivoRequest r) {
        DispositivoPush dispositivo = dispositivoRepository.findByExpoToken(r.expoToken())
                .orElseGet(() -> new DispositivoPush(r.expoToken()));
        dispositivo.asignar(personaRepository.getReferenceById(personaId), r.plataforma(), OffsetDateTime.now(clock));
        dispositivoRepository.save(dispositivo);
    }

    /** Al cerrar sesión; solo da de baja tokens propios y no falla si ya no existe. */
    @Transactional
    public void eliminar(Long personaId, String expoToken) {
        dispositivoRepository.eliminarDePersona(expoToken, personaId);
    }
}
