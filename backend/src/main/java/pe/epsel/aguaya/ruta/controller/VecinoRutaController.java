package pe.epsel.aguaya.ruta.controller;

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
import pe.epsel.aguaya.ruta.dto.AtencionVecinoResponse;
import pe.epsel.aguaya.ruta.service.VecinoRutaService;

@Tag(name = "Vecino - Atención")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT)
@RestController
@RequestMapping("/api/vecino")
@RequiredArgsConstructor
public class VecinoRutaController {

    private final VecinoRutaService vecinoRutaService;

    @Operation(summary = "Próxima atención (ruta en curso o siguiente programada) y última, con su parada")
    @GetMapping("/proxima-atencion")
    public AtencionVecinoResponse proximaAtencion(@AuthenticationPrincipal UsuarioAutenticado vecino) {
        return vecinoRutaService.proximaAtencion(vecino.id());
    }
}
