package pe.epsel.aguaya.incidencia.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.epsel.aguaya.common.config.OpenApiConfig;
import pe.epsel.aguaya.common.dto.PaginaResponse;
import pe.epsel.aguaya.common.seguridad.UsuarioAutenticado;
import pe.epsel.aguaya.incidencia.dto.IncidenciaResponse;
import pe.epsel.aguaya.incidencia.dto.ReasignacionResponse;
import pe.epsel.aguaya.incidencia.dto.ReasignarRequest;
import pe.epsel.aguaya.incidencia.dto.SugerenciasReasignacionResponse;
import pe.epsel.aguaya.incidencia.entity.EstadoIncidencia;
import pe.epsel.aguaya.incidencia.entity.TipoIncidencia;
import pe.epsel.aguaya.incidencia.service.IncidenciaService;
import pe.epsel.aguaya.incidencia.service.ReasignacionService;

@Tag(name = "Admin - Incidencias")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT)
@RestController
@RequestMapping("/api/admin/incidencias")
@RequiredArgsConstructor
public class AdminIncidenciaController {

    private final IncidenciaService incidenciaService;
    private final ReasignacionService reasignacionService;

    @Operation(summary = "Lista incidencias (filtros opcionales: estado, tipo, rutaId); las más recientes primero")
    @GetMapping
    public PaginaResponse<IncidenciaResponse> listar(@RequestParam(required = false) EstadoIncidencia estado,
                                                     @RequestParam(required = false) TipoIncidencia tipo,
                                                     @RequestParam(required = false) Long rutaId,
                                                     @PageableDefault(sort = "creadoEn", direction = Sort.Direction.DESC)
                                                     Pageable pageable) {
        return incidenciaService.listar(estado, tipo, rutaId, pageable);
    }

    @Operation(summary = "Obtiene una incidencia")
    @GetMapping("/{id}")
    public IncidenciaResponse obtener(@PathVariable Long id) {
        return incidenciaService.obtener(id);
    }

    @Operation(summary = "Rutas candidatas para recibir las paradas pendientes (RF07); el administrador elige")
    @GetMapping("/{id}/sugerencias-reasignacion")
    public SugerenciasReasignacionResponse sugerencias(@PathVariable Long id) {
        return reasignacionService.sugerencias(id);
    }

    @Operation(summary = "Mueve las paradas pendientes de la ruta de la incidencia a la ruta destino (RF07)")
    @PostMapping("/{id}/reasignar")
    public ReasignacionResponse reasignar(@PathVariable Long id, @Valid @RequestBody ReasignarRequest request,
                                          @AuthenticationPrincipal UsuarioAutenticado admin) {
        return reasignacionService.reasignar(id, admin.id(), request);
    }

    @Operation(summary = "Cierra la incidencia como resuelta")
    @PatchMapping("/{id}/cerrar")
    public IncidenciaResponse cerrar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado admin) {
        return incidenciaService.cerrar(id, admin.id());
    }
}
