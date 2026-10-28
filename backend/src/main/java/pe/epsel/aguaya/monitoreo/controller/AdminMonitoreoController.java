package pe.epsel.aguaya.monitoreo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.epsel.aguaya.common.config.OpenApiConfig;
import pe.epsel.aguaya.monitoreo.dto.RecorridoResponse;
import pe.epsel.aguaya.monitoreo.service.MonitoreoService;

@Tag(name = "Admin - Monitoreo")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT)
@RestController
@RequestMapping("/api/admin/rutas")
@RequiredArgsConstructor
public class AdminMonitoreoController {

    private final MonitoreoService monitoreoService;

    @Operation(summary = "Recorrido GPS de la ruta e intentos de atención rechazados fuera de la geocerca")
    @GetMapping("/{id}/recorrido")
    public RecorridoResponse recorrido(@PathVariable Long id) {
        return monitoreoService.recorrido(id);
    }
}
