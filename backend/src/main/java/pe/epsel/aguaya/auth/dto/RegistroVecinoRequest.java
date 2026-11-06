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
 * Registro del vecino: el código SMS confirma el teléfono, los nombres se obtienen del DNI y el vecino elige una
 * clave de 4 dígitos (como Yape o Plin) con la que ingresa después junto con su DNI. La ubicación
 * (latitud/longitud) la toma la app del GPS del celular; {@code direccionReferencia} es texto libre opcional de
 * apoyo para el conductor.
 */
public record RegistroVecinoRequest(
        @NotBlank @Pattern(regexp = "^\\d{8}$", message = "debe tener 8 dígitos") String dni,
        @NotBlank @Pattern(regexp = Telefono.PATRON, message = "debe ser un celular peruano (9 dígitos)")
        String telefono,
        @NotBlank @Pattern(regexp = "^\\d{4,10}$", message = "código inválido") String codigo,
        @NotBlank @Pattern(regexp = PATRON_CLAVE, message = "debe tener 4 dígitos") String clave,
        @Size(max = 255) String direccionReferencia,
        @NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal latitud,
        @NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal longitud) {

    /** Clave del vecino: 4 dígitos. */
    public static final String PATRON_CLAVE = "^\\d{4}$";
}
