package pe.epsel.aguaya.calificacion.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.epsel.aguaya.calificacion.dto.CalificacionResponse;
import pe.epsel.aguaya.calificacion.dto.ResumenCalificacionesResponse;
import pe.epsel.aguaya.calificacion.service.CalificacionService;
import pe.epsel.aguaya.common.config.OpenApiConfig;
import pe.epsel.aguaya.common.dto.PaginaResponse;

@Tag(name = "Admin - Calificaciones")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT)
@RestController
@RequestMapping("/api/admin/calificaciones")
@RequiredArgsConstructor
public class AdminCalificacionController {

    private final CalificacionService calificacionService;

    @Operation(summary = "Calificaciones con su comentario (filtro opcional: sectorId); las más recientes primero")
    @GetMapping
    public PaginaResponse<CalificacionResponse> listar(@RequestParam(required = false) Long sectorId,
                                                       @PageableDefault(sort = "creadoEn", direction = Sort.Direction.DESC)
                                                       Pageable pageable) {
        return calificacionService.listar(sectorId, pageable);
    }

    @Operation(summary = "Promedio, distribución por estrellas y promedio por conductor")
    @GetMapping("/resumen")
    public ResumenCalificacionesResponse resumen(@RequestParam(required = false) Long sectorId) {
        return calificacionService.resumen(sectorId);
    }
}
