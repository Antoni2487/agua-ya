package pe.epsel.aguaya.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * El vecino pide el código SMS para ingresar; llega al celular con el que se registró.
 */
public record SolicitarCodigoLoginRequest(
        @NotBlank @Pattern(regexp = "^\\d{8}$", message = "debe tener 8 dígitos") String dni) {
}
