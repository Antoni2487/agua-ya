package pe.epsel.aguaya.ruta.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import pe.epsel.aguaya.common.config.OpenApiConfig;
import pe.epsel.aguaya.common.seguridad.UsuarioAutenticado;
import pe.epsel.aguaya.ruta.dto.IniciarViajeRequest;
import pe.epsel.aguaya.ruta.dto.ViajeResponse;
import pe.epsel.aguaya.ruta.service.ViajeService;

@Tag(name = "Conductor - Viajes")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT)
@RestController
@RequestMapping("/api/conductor")
@RequiredArgsConstructor
public class ConductorViajeController {

    private final ViajeService viajeService;

    @Operation(summary = "Registra la salida cargada de un viaje (ruta EN_CURSO, un viaje abierto a la vez)")
    @PostMapping("/rutas/{id}/viajes")
    @ResponseStatus(HttpStatus.CREATED)
    public ViajeResponse iniciar(@PathVariable Long id, @Valid @RequestBody IniciarViajeRequest request,
                                 @AuthenticationPrincipal UsuarioAutenticado conductor) {
        return viajeService.iniciar(id, conductor.id(), request);
    }

    @Operation(summary = "Finaliza el viaje en reparto (EN_REPARTO -> FINALIZADO)")
    @PatchMapping("/viajes/{id}/finalizar")
    public ViajeResponse finalizar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado conductor) {
        return viajeService.finalizar(id, conductor.id());
    }
}
