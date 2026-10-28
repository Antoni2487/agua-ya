package pe.epsel.aguaya.capacidad.controller;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.epsel.aguaya.capacidad.dto.CapacidadResponse;
import pe.epsel.aguaya.capacidad.service.CapacidadService;
import pe.epsel.aguaya.common.config.OpenApiConfig;

@Tag(name = "Admin - Capacidad")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT)
@RestController
@RequestMapping("/api/admin/capacidad")
@RequiredArgsConstructor
public class CapacidadController {

    private final CapacidadService capacidadService;

    @Operation(summary = "Viajes y cisternas que necesitan los sectores frente a la flota disponible, con déficit (RF02)",
            description = "Sin sectorId considera todos los sectores activos compartiendo la flota. "
                    + "fecha por defecto: hoy. capacidadLitros simula con otro tamaño de cisterna.")
    @GetMapping
    public CapacidadResponse calcular(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) Long sectorId,
            @RequestParam(required = false) Integer capacidadLitros) {
        return capacidadService.calcular(fecha, sectorId, capacidadLitros);
    }
}
