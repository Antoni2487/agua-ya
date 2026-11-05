package pe.epsel.aguaya.notificacion.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import pe.epsel.aguaya.common.config.OpenApiConfig;
import pe.epsel.aguaya.common.seguridad.UsuarioAutenticado;
import pe.epsel.aguaya.notificacion.dto.BajaWebPushRequest;
import pe.epsel.aguaya.notificacion.dto.ClaveWebPushResponse;
import pe.epsel.aguaya.notificacion.dto.SuscripcionWebPushRequest;
import pe.epsel.aguaya.notificacion.service.WebPushService;

@Tag(name = "Notificaciones")
@RestController
@RequiredArgsConstructor
public class WebPushController {

    private final WebPushService webPushService;

    @Operation(summary = "Clave VAPID pública para suscribir el navegador (applicationServerKey)")
    @GetMapping("/api/public/web-push/clave-publica")
    public ClaveWebPushResponse clavePublica() {
        return webPushService.clavePublica();
    }

    @Operation(summary = "Registra la suscripción Web Push del navegador del vecino")
    @SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT)
    @PostMapping("/api/vecino/web-push")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void suscribir(@Valid @RequestBody SuscripcionWebPushRequest request,
                          @AuthenticationPrincipal UsuarioAutenticado persona) {
        webPushService.suscribir(persona.id(), request);
    }

    @Operation(summary = "Da de baja la suscripción Web Push (al cerrar sesión o desactivar avisos)")
    @SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT)
    @DeleteMapping("/api/vecino/web-push")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void darDeBaja(@Valid @RequestBody BajaWebPushRequest request,
                          @AuthenticationPrincipal UsuarioAutenticado persona) {
        webPushService.darDeBaja(persona.id(), request.endpoint());
    }
}
