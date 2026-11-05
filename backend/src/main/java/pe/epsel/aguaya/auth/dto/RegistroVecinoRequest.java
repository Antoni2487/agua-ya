package pe.epsel.aguaya.auth.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import pe.epsel.aguaya.common.util.Telefono;

/**
 * Registro del vecino, sin contraseña: el código SMS confirma el teléfono y los nombres se obtienen del DNI.
 * Después ingresa con DNI + código SMS. La ubicación (latitud/longitud) la toma la web del GPS del celular;
 * {@code direccionReferencia} es texto libre opcional de apoyo para el conductor.
 */
public record RegistroVecinoRequest(
        @NotBlank @Pattern(regexp = "^\\d{8}$", message = "debe tener 8 dígitos") String dni,
        @NotBlank @Pattern(regexp = Telefono.PATRON, message = "debe ser un celular peruano (9 dígitos)")
        String telefono,
        @NotBlank @Pattern(regexp = "^\\d{4,10}$", message = "código inválido") String codigo,
        @Size(max = 255) String direccionReferencia,
        @NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal latitud,
        @NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal longitud) {
}
