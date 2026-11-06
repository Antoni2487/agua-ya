package pe.epsel.aguaya.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * El vecino que olvidó su clave pide el código SMS ({@code /login/codigo/solicitar}) y con él elige una nueva.
 */
public record RestablecerClaveRequest(
        @NotBlank @Pattern(regexp = "^\\d{8}$", message = "debe tener 8 dígitos") String dni,
        @NotBlank @Pattern(regexp = "^\\d{4,10}$", message = "código inválido") String codigo,
        @NotBlank @Pattern(regexp = RegistroVecinoRequest.PATRON_CLAVE, message = "debe tener 4 dígitos") String clave) {
}
