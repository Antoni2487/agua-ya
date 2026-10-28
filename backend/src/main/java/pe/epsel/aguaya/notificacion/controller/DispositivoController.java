package pe.epsel.aguaya.notificacion.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import pe.epsel.aguaya.common.config.OpenApiConfig;
import pe.epsel.aguaya.common.seguridad.UsuarioAutenticado;
import pe.epsel.aguaya.notificacion.dto.DispositivoRequest;
import pe.epsel.aguaya.notificacion.dto.RegistrarDispositivoRequest;
import pe.epsel.aguaya.notificacion.service.DispositivoService;

@Tag(name = "Notificaciones")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT)
@RestController
@RequestMapping("/api/dispositivos")
@RequiredArgsConstructor
public class DispositivoController {

    private final DispositivoService dispositivoService;

    @Operation(summary = "Registra el token de Expo del celular para recibir notificaciones push")
    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registrar(@Valid @RequestBody RegistrarDispositivoRequest request,
                          @AuthenticationPrincipal UsuarioAutenticado persona) {
        dispositivoService.registrar(persona.id(), request);
    }

    @Operation(summary = "Da de baja el token del celular (al cerrar sesión)")
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@Valid @RequestBody DispositivoRequest request,
                         @AuthenticationPrincipal UsuarioAutenticado persona) {
        dispositivoService.eliminar(persona.id(), request.expoToken());
    }
}
