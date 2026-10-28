package pe.epsel.aguaya.sector.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import pe.epsel.aguaya.common.config.OpenApiConfig;
import pe.epsel.aguaya.common.dto.PaginaResponse;
import pe.epsel.aguaya.sector.dto.InstitucionRequest;
import pe.epsel.aguaya.sector.dto.InstitucionResponse;
import pe.epsel.aguaya.sector.entity.TipoInstitucion;
import pe.epsel.aguaya.sector.service.InstitucionService;

@Tag(name = "Admin - Instituciones críticas")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT)
@RestController
@RequestMapping("/api/admin/instituciones")
@RequiredArgsConstructor
public class InstitucionController {

    private final InstitucionService institucionService;

    @Operation(summary = "Lista instituciones críticas (filtros opcionales: sectorId, tipo, activo)")
    @GetMapping
    public PaginaResponse<InstitucionResponse> listar(@RequestParam(required = false) Long sectorId,
                                                      @RequestParam(required = false) TipoInstitucion tipo,
                                                      @RequestParam(required = false) Boolean activo,
                                                      @PageableDefault(sort = "id", direction = Sort.Direction.ASC)
                                                      Pageable pageable) {
        return institucionService.listar(sectorId, tipo, activo, pageable);
    }

    @Operation(summary = "Obtiene una institución crítica")
    @GetMapping("/{id}")
    public InstitucionResponse obtener(@PathVariable Long id) {
        return institucionService.obtener(id);
    }

    @Operation(summary = "Crea una institución crítica")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InstitucionResponse crear(@Valid @RequestBody InstitucionRequest request) {
        return institucionService.crear(request);
    }

    @Operation(summary = "Actualiza una institución crítica")
    @PutMapping("/{id}")
    public InstitucionResponse actualizar(@PathVariable Long id, @Valid @RequestBody InstitucionRequest request) {
        return institucionService.actualizar(id, request);
    }

    @Operation(summary = "Desactiva una institución crítica (baja lógica)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(@PathVariable Long id) {
        institucionService.desactivar(id);
    }
}
