package pe.epsel.aguaya.calificacion.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import pe.epsel.aguaya.calificacion.dto.CalificacionResponse;
import pe.epsel.aguaya.calificacion.dto.CalificarRequest;
import pe.epsel.aguaya.calificacion.service.CalificacionService;
import pe.epsel.aguaya.common.config.OpenApiConfig;
import pe.epsel.aguaya.common.seguridad.UsuarioAutenticado;

@Tag(name = "Vecino - Calificaciones")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT)
@RestController
@RequestMapping("/api/vecino/calificaciones")
@RequiredArgsConstructor
public class VecinoCalificacionController {

    private final CalificacionService calificacionService;

    @Operation(summary = "Califica la entrega en su parada atendida (1 a 5 estrellas, una vez) (RF08)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CalificacionResponse calificar(@Valid @RequestBody CalificarRequest request,
                                          @AuthenticationPrincipal UsuarioAutenticado vecino) {
        return calificacionService.calificar(vecino.id(), request);
    }
}
