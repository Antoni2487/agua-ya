package pe.epsel.aguaya.monitoreo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.epsel.aguaya.common.config.OpenApiConfig;
import pe.epsel.aguaya.common.seguridad.UsuarioAutenticado;
import pe.epsel.aguaya.monitoreo.dto.PosicionResponse;
import pe.epsel.aguaya.monitoreo.service.MonitoreoService;

@Tag(name = "Vecino - Cisterna en camino")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT)
@RestController
@RequestMapping("/api/vecino/rutas")
@RequiredArgsConstructor
public class VecinoMonitoreoController {

    private final MonitoreoService monitoreoService;

    @Operation(summary = "Última posición de la cisterna de una ruta de su sector, solo mientras está EN_CURSO")
    @GetMapping("/{id}/posicion")
    public PosicionResponse posicion(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado vecino) {
        return monitoreoService.posicionParaVecino(id, vecino.id());
    }
}
