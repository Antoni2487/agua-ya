package pe.epsel.aguaya.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import pe.epsel.aguaya.common.util.Telefono;

/**
 * Pide el código que confirma el teléfono antes del registro.
 *
 * @param dni opcional; permite avisar antes de enviar el SMS si el DNI ya está registrado
 */
public record SolicitarOtpRequest(
        @NotBlank @Pattern(regexp = Telefono.PATRON, message = "debe ser un celular peruano (9 dígitos)")
        String telefono,
        @Pattern(regexp = "^\\d{8}$", message = "debe tener 8 dígitos") String dni) {
}
