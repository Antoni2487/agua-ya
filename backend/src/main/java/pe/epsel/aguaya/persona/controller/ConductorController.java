package pe.epsel.aguaya.persona.controller;

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
import pe.epsel.aguaya.common.dominio.Turno;
import pe.epsel.aguaya.common.dto.PaginaResponse;
import pe.epsel.aguaya.persona.dto.ConductorActualizarRequest;
import pe.epsel.aguaya.persona.dto.ConductorCrearRequest;
import pe.epsel.aguaya.persona.dto.ConductorResponse;
import pe.epsel.aguaya.persona.entity.EstadoDisponibilidad;
import pe.epsel.aguaya.persona.service.ConductorService;

@Tag(name = "Admin - Conductores")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT)
@RestController
@RequestMapping("/api/admin/conductores")
@RequiredArgsConstructor
public class ConductorController {

    private final ConductorService conductorService;

    @Operation(summary = "Lista conductores (filtros opcionales: estado, turno, activo)")
    @GetMapping
    public PaginaResponse<ConductorResponse> listar(@RequestParam(required = false) EstadoDisponibilidad estado,
                                                    @RequestParam(required = false) Turno turno,
                                                    @RequestParam(required = false) Boolean activo,
                                                    @PageableDefault(sort = "id", direction = Sort.Direction.ASC)
                                                    Pageable pageable) {
        return conductorService.listar(estado, turno, activo, pageable);
    }

    @Operation(summary = "Obtiene un conductor")
    @GetMapping("/{id}")
    public ConductorResponse obtener(@PathVariable Long id) {
        return conductorService.obtener(id);
    }

    @Operation(summary = "Crea la cuenta de un conductor (ingresa con DNI + contraseña)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConductorResponse crear(@Valid @RequestBody ConductorCrearRequest request) {
        return conductorService.crear(request);
    }

    @Operation(summary = "Actualiza un conductor (datos, turno, disponibilidad o contraseña)")
    @PutMapping("/{id}")
    public ConductorResponse actualizar(@PathVariable Long id, @Valid @RequestBody ConductorActualizarRequest request) {
        return conductorService.actualizar(id, request);
    }

    @Operation(summary = "Desactiva un conductor y cierra sus sesiones (baja lógica)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(@PathVariable Long id) {
        conductorService.desactivar(id);
    }
}
