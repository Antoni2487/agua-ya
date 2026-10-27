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
import pe.epsel.aguaya.sector.dto.PuntoEntregaRequest;
import pe.epsel.aguaya.sector.dto.PuntoEntregaResponse;
import pe.epsel.aguaya.sector.service.PuntoEntregaService;

@Tag(name = "Admin - Puntos de entrega")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT)
@RestController
@RequestMapping("/api/admin/puntos-entrega")
@RequiredArgsConstructor
public class PuntoEntregaController {

    private final PuntoEntregaService puntoEntregaService;

    @Operation(summary = "Lista puntos de entrega (filtros opcionales: sectorId, activo)")
    @GetMapping
    public PaginaResponse<PuntoEntregaResponse> listar(@RequestParam(required = false) Long sectorId,
                                                      @RequestParam(required = false) Boolean activo,
                                                      @PageableDefault(sort = "id", direction = Sort.Direction.ASC)
                                                      Pageable pageable) {
        return puntoEntregaService.listar(sectorId, activo, pageable);
    }

    @Operation(summary = "Obtiene un punto de entrega")
    @GetMapping("/{id}")
    public PuntoEntregaResponse obtener(@PathVariable Long id) {
        return puntoEntregaService.obtener(id);
    }

    @Operation(summary = "Crea un punto de entrega")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PuntoEntregaResponse crear(@Valid @RequestBody PuntoEntregaRequest request) {
        return puntoEntregaService.crear(request);
    }

    @Operation(summary = "Actualiza un punto de entrega")
    @PutMapping("/{id}")
    public PuntoEntregaResponse actualizar(@PathVariable Long id, @Valid @RequestBody PuntoEntregaRequest request) {
        return puntoEntregaService.actualizar(id, request);
    }

    @Operation(summary = "Desactiva un punto de entrega (baja lógica)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(@PathVariable Long id) {
        puntoEntregaService.desactivar(id);
    }
}
