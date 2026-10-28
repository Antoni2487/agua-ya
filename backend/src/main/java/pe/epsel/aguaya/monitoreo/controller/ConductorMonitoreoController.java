package pe.epsel.aguaya.monitoreo.controller;

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
import pe.epsel.aguaya.monitoreo.dto.AtenderParadaRequest;
import pe.epsel.aguaya.monitoreo.dto.PosicionRequest;
import pe.epsel.aguaya.monitoreo.service.GeocercaService;
import pe.epsel.aguaya.monitoreo.service.MonitoreoService;
import pe.epsel.aguaya.ruta.dto.ParadaResponse;

@Tag(name = "Conductor - Monitoreo")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT)
@RestController
@RequestMapping("/api/conductor")
@RequiredArgsConstructor
public class ConductorMonitoreoController {

    private final MonitoreoService monitoreoService;
    private final GeocercaService geocercaService;

    @Operation(summary = "Envía la posición GPS del conductor (cada ~15 s mientras la ruta está EN_CURSO)")
    @PostMapping("/rutas/{id}/posicion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void posicion(@PathVariable Long id, @Valid @RequestBody PosicionRequest request,
                         @AuthenticationPrincipal UsuarioAutenticado conductor) {
        monitoreoService.registrarPosicion(id, conductor.id(), request);
    }

    @Operation(summary = "Marca la parada como atendida si el conductor está dentro de la geocerca (RF05)")
    @PatchMapping("/paradas/{id}/atender")
    public ParadaResponse atender(@PathVariable Long id, @Valid @RequestBody(required = false) AtenderParadaRequest request,
                                  @AuthenticationPrincipal UsuarioAutenticado conductor) {
        return geocercaService.atender(id, conductor.id(), request);
    }

    @Operation(summary = "Marca la parada como no atendida")
    @PatchMapping("/paradas/{id}/no-atendida")
    public ParadaResponse noAtendida(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado conductor) {
        return geocercaService.noAtendida(id, conductor.id());
    }
}
