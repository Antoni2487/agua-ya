package pe.epsel.aguaya.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import pe.epsel.aguaya.auth.entity.PropositoOtp;
import pe.epsel.aguaya.common.util.Telefono;

/**
 * @param dni opcional; en REGISTRO permite avisar antes de enviar el SMS si el DNI ya está registrado
 */
public record SolicitarOtpRequest(
        @NotBlank @Pattern(regexp = Telefono.PATRON, message = "debe ser un celular peruano (9 dígitos)")
        String telefono,
        @NotNull PropositoOtp proposito,
        @Pattern(regexp = "^\\d{8}$", message = "debe tener 8 dígitos") String dni) {
}
