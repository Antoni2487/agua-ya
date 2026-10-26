package pe.epsel.aguaya.persona.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import pe.epsel.aguaya.common.dominio.Turno;
import pe.epsel.aguaya.common.util.Telefono;

/**
 * Alta de conductor por el administrador. El conductor ingresa luego con DNI + {@code password}.
 */
public record ConductorCrearRequest(
        @NotBlank @Pattern(regexp = "^\\d{8}$", message = "debe tener 8 dígitos") String dni,
        @NotBlank @Size(max = 100) String nombres,
        @NotBlank @Size(max = 100) String apellidos,
        @NotBlank @Pattern(regexp = Telefono.PATRON, message = "debe ser un celular peruano (9 dígitos)")
        String telefono,
        @Email @Size(max = 150) String email,
        @NotBlank @Size(min = 8, max = 72, message = "debe tener entre 8 y 72 caracteres") String password,
        @NotBlank @Size(max = 20) String licencia,
        Turno turno) {
}
