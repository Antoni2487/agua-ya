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
import pe.epsel.aguaya.sector.dto.SectorRequest;
import pe.epsel.aguaya.sector.dto.SectorResponse;
import pe.epsel.aguaya.sector.service.SectorService;

@Tag(name = "Admin - Sectores")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT)
@RestController
@RequestMapping("/api/admin/sectores")
@RequiredArgsConstructor
public class SectorController {

    private final SectorService sectorService;

    @Operation(summary = "Lista sectores (filtros opcionales: distrito, activo)")
    @GetMapping
    public PaginaResponse<SectorResponse> listar(@RequestParam(required = false) String distrito,
                                                 @RequestParam(required = false) Boolean activo,
                                                 @PageableDefault(sort = "id", direction = Sort.Direction.ASC)
                                                 Pageable pageable) {
        return sectorService.listar(distrito, activo, pageable);
    }

    @Operation(summary = "Obtiene un sector")
    @GetMapping("/{id}")
    public SectorResponse obtener(@PathVariable Long id) {
        return sectorService.obtener(id);
    }

    @Operation(summary = "Crea un sector")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SectorResponse crear(@Valid @RequestBody SectorRequest request) {
        return sectorService.crear(request);
    }

    @Operation(summary = "Actualiza un sector")
    @PutMapping("/{id}")
    public SectorResponse actualizar(@PathVariable Long id, @Valid @RequestBody SectorRequest request) {
        return sectorService.actualizar(id, request);
    }

    @Operation(summary = "Desactiva un sector (baja lógica)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(@PathVariable Long id) {
        sectorService.desactivar(id);
    }
}
