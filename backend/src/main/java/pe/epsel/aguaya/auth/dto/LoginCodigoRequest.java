package pe.epsel.aguaya.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Ingreso del vecino con DNI + el código SMS recibido.
 */
public record LoginCodigoRequest(
        @NotBlank @Pattern(regexp = "^\\d{8}$", message = "debe tener 8 dígitos") String dni,
        @NotBlank @Pattern(regexp = "^\\d{4,10}$", message = "código inválido") String codigo) {
}
