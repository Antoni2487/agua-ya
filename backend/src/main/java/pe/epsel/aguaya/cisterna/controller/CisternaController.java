package pe.epsel.aguaya.cisterna.controller;

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

import pe.epsel.aguaya.cisterna.dto.CisternaRequest;
import pe.epsel.aguaya.cisterna.dto.CisternaResponse;
import pe.epsel.aguaya.cisterna.entity.EstadoVehiculo;
import pe.epsel.aguaya.cisterna.service.CisternaService;
import pe.epsel.aguaya.common.config.OpenApiConfig;
import pe.epsel.aguaya.common.dto.PaginaResponse;

@Tag(name = "Admin - Cisternas")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT)
@RestController
@RequestMapping("/api/admin/cisternas")
@RequiredArgsConstructor
public class CisternaController {

    private final CisternaService cisternaService;

    @Operation(summary = "Lista cisternas (filtro opcional: estado)")
    @GetMapping
    public PaginaResponse<CisternaResponse> listar(@RequestParam(required = false) EstadoVehiculo estado,
                                                   @PageableDefault(sort = "id", direction = Sort.Direction.ASC)
                                                   Pageable pageable) {
        return cisternaService.listar(estado, pageable);
    }

    @Operation(summary = "Obtiene una cisterna")
    @GetMapping("/{id}")
    public CisternaResponse obtener(@PathVariable Long id) {
        return cisternaService.obtener(id);
    }

    @Operation(summary = "Registra una cisterna")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CisternaResponse crear(@Valid @RequestBody CisternaRequest request) {
        return cisternaService.crear(request);
    }

    @Operation(summary = "Actualiza una cisterna (datos, estado o autorización sanitaria)")
    @PutMapping("/{id}")
    public CisternaResponse actualizar(@PathVariable Long id, @Valid @RequestBody CisternaRequest request) {
        return cisternaService.actualizar(id, request);
    }

    @Operation(summary = "Elimina una cisterna sin rutas registradas")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        cisternaService.eliminar(id);
    }
}
