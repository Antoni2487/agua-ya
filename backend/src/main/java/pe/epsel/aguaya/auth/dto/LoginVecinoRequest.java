package pe.epsel.aguaya.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import pe.epsel.aguaya.common.util.Telefono;

public record LoginVecinoRequest(
        @NotBlank @Pattern(regexp = Telefono.PATRON, message = "debe ser un celular peruano (9 dígitos)")
        String telefono,
        @NotBlank @Pattern(regexp = "^\\d{6}$", message = "debe tener 6 dígitos") String codigo) {
}
