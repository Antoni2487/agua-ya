package pe.epsel.aguaya.persona.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.epsel.aguaya.common.config.OpenApiConfig;
import pe.epsel.aguaya.common.seguridad.UsuarioAutenticado;
import pe.epsel.aguaya.persona.dto.VecinoPerfilResponse;
import pe.epsel.aguaya.persona.service.VecinoService;

@Tag(name = "Vecino - Perfil")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT)
@RestController
@RequestMapping("/api/vecino")
@RequiredArgsConstructor
public class VecinoController {

    private final VecinoService vecinoService;

    @Operation(summary = "Datos del vecino autenticado, con su vivienda y su sector")
    @GetMapping("/me")
    public VecinoPerfilResponse me(@AuthenticationPrincipal UsuarioAutenticado vecino) {
        return vecinoService.perfil(vecino.id());
    }
}
