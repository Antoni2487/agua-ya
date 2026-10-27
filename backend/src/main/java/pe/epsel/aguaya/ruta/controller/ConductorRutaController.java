package pe.epsel.aguaya.ruta.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.epsel.aguaya.common.config.OpenApiConfig;
import pe.epsel.aguaya.common.seguridad.UsuarioAutenticado;
import pe.epsel.aguaya.ruta.dto.RutaDetalleResponse;
import pe.epsel.aguaya.ruta.service.ConductorRutaService;

@Tag(name = "Conductor - Rutas")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT)
@RestController
@RequestMapping("/api/conductor/rutas")
@RequiredArgsConstructor
public class ConductorRutaController {

    private final ConductorRutaService conductorRutaService;

    @Operation(summary = "Rutas asignadas al conductor para hoy, con sus paradas en orden")
    @GetMapping("/hoy")
    public List<RutaDetalleResponse> hoy(@AuthenticationPrincipal UsuarioAutenticado conductor) {
        return conductorRutaService.rutasDeHoy(conductor.id());
    }

    @Operation(summary = "Inicia la ruta (PROGRAMADA -> EN_CURSO); desde aquí se envía el GPS")
    @PatchMapping("/{id}/iniciar")
    public RutaDetalleResponse iniciar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado conductor) {
        return conductorRutaService.iniciar(id, conductor.id());
    }

    @Operation(summary = "Finaliza la ruta (EN_CURSO -> COMPLETADA); exige todas las paradas en estado final")
    @PatchMapping("/{id}/finalizar")
    public RutaDetalleResponse finalizar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado conductor) {
        return conductorRutaService.finalizar(id, conductor.id());
    }
}
