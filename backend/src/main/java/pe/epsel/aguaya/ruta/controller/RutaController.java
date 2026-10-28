package pe.epsel.aguaya.ruta.controller;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import pe.epsel.aguaya.common.config.OpenApiConfig;
import pe.epsel.aguaya.common.dto.PaginaResponse;
import pe.epsel.aguaya.common.seguridad.UsuarioAutenticado;
import pe.epsel.aguaya.ruta.dto.AsignarRutaRequest;
import pe.epsel.aguaya.ruta.dto.ProgramarRutaRequest;
import pe.epsel.aguaya.ruta.dto.RutaDetalleResponse;
import pe.epsel.aguaya.ruta.dto.RutaResumenResponse;
import pe.epsel.aguaya.ruta.dto.SugerenciasAsignacionResponse;
import pe.epsel.aguaya.ruta.entity.EstadoRuta;
import pe.epsel.aguaya.ruta.service.AsignacionRutaService;
import pe.epsel.aguaya.ruta.service.RutaService;

@Tag(name = "Admin - Rutas")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT)
@RestController
@RequestMapping("/api/admin/rutas")
@RequiredArgsConstructor
public class RutaController {

    private final RutaService rutaService;
    private final AsignacionRutaService asignacionRutaService;

    @Operation(summary = "Programa una ruta (RF01): genera y ordena sus paradas")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RutaDetalleResponse programar(@Valid @RequestBody ProgramarRutaRequest request,
                                         @AuthenticationPrincipal UsuarioAutenticado admin) {
        return rutaService.programar(request, admin.id());
    }

    @Operation(summary = "Lista rutas (filtros opcionales: fecha, estado, sectorId)")
    @GetMapping
    public PaginaResponse<RutaResumenResponse> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) EstadoRuta estado,
            @RequestParam(required = false) Long sectorId,
            @PageableDefault(sort = {"fechaProgramada", "id"}, direction = Sort.Direction.DESC) Pageable pageable) {
        return rutaService.listar(fecha, estado, sectorId, pageable);
    }

    @Operation(summary = "Detalle de una ruta con sus paradas en orden de recorrido")
    @GetMapping("/{id}")
    public RutaDetalleResponse obtener(@PathVariable Long id) {
        return rutaService.obtener(id);
    }

    @Operation(summary = "Sugiere cisternas y conductores para la ruta (RF03)")
    @GetMapping("/{id}/sugerencias-asignacion")
    public SugerenciasAsignacionResponse sugerencias(@PathVariable Long id) {
        return asignacionRutaService.sugerencias(id);
    }

    @Operation(summary = "Asigna cisterna, conductor y viajes planificados (RF03, regla 2)")
    @PatchMapping("/{id}/asignar")
    public RutaDetalleResponse asignar(@PathVariable Long id, @Valid @RequestBody AsignarRutaRequest request) {
        return asignacionRutaService.asignar(id, request);
    }

    @Operation(summary = "Elimina una ruta que aún no ha iniciado")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        rutaService.eliminar(id);
    }
}
