package pe.epsel.aguaya.puntollenado.controller;

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
import pe.epsel.aguaya.puntollenado.dto.PuntoLlenadoRequest;
import pe.epsel.aguaya.puntollenado.dto.PuntoLlenadoResponse;
import pe.epsel.aguaya.puntollenado.entity.TipoPuntoLlenado;
import pe.epsel.aguaya.puntollenado.service.PuntoLlenadoService;

@Tag(name = "Admin - Puntos de llenado")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT)
@RestController
@RequestMapping("/api/admin/puntos-llenado")
@RequiredArgsConstructor
public class PuntoLlenadoController {

    private final PuntoLlenadoService puntoLlenadoService;

    @Operation(summary = "Lista puntos de llenado (filtros opcionales: tipo, activo)")
    @GetMapping
    public PaginaResponse<PuntoLlenadoResponse> listar(@RequestParam(required = false) TipoPuntoLlenado tipo,
                                                      @RequestParam(required = false) Boolean activo,
                                                      @PageableDefault(sort = "id", direction = Sort.Direction.ASC)
                                                      Pageable pageable) {
        return puntoLlenadoService.listar(tipo, activo, pageable);
    }

    @Operation(summary = "Obtiene un punto de llenado")
    @GetMapping("/{id}")
    public PuntoLlenadoResponse obtener(@PathVariable Long id) {
        return puntoLlenadoService.obtener(id);
    }

    @Operation(summary = "Crea un punto de llenado")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PuntoLlenadoResponse crear(@Valid @RequestBody PuntoLlenadoRequest request) {
        return puntoLlenadoService.crear(request);
    }

    @Operation(summary = "Actualiza un punto de llenado")
    @PutMapping("/{id}")
    public PuntoLlenadoResponse actualizar(@PathVariable Long id, @Valid @RequestBody PuntoLlenadoRequest request) {
        return puntoLlenadoService.actualizar(id, request);
    }

    @Operation(summary = "Desactiva un punto de llenado (baja lógica)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(@PathVariable Long id) {
        puntoLlenadoService.desactivar(id);
    }
}
