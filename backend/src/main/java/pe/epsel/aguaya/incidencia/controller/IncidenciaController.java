package pe.epsel.aguaya.incidencia.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import pe.epsel.aguaya.common.config.OpenApiConfig;
import pe.epsel.aguaya.common.dto.PaginaResponse;
import pe.epsel.aguaya.common.seguridad.UsuarioAutenticado;
import pe.epsel.aguaya.incidencia.dto.IncidenciaResponse;
import pe.epsel.aguaya.incidencia.dto.ReportarIncidenciaRequest;
import pe.epsel.aguaya.incidencia.service.IncidenciaService;

@Tag(name = "Incidencias (conductor y vecino)")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT)
@RestController
@RequestMapping("/api/incidencias")
@RequiredArgsConstructor
public class IncidenciaController {

    private final IncidenciaService incidenciaService;

    @Operation(summary = "Reporta una incidencia sobre una ruta (RF06); alerta al administrador en tiempo real")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IncidenciaResponse reportar(@Valid @RequestBody ReportarIncidenciaRequest request,
                                       @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return incidenciaService.reportar(usuario, request);
    }

    @Operation(summary = "Incidencias que reportó la persona autenticada, con su estado")
    @GetMapping
    public PaginaResponse<IncidenciaResponse> mias(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                   @PageableDefault(sort = "creadoEn", direction = Sort.Direction.DESC)
                                                   Pageable pageable) {
        return incidenciaService.misIncidencias(usuario.id(), pageable);
    }
}
