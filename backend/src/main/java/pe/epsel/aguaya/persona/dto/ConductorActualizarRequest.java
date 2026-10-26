package pe.epsel.aguaya.persona.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import pe.epsel.aguaya.common.dominio.Turno;
import pe.epsel.aguaya.common.util.Telefono;
import pe.epsel.aguaya.persona.entity.EstadoDisponibilidad;

/**
 * Edición de conductor. El DNI no se cambia.
 *
 * @param estadoDisponibilidad solo DISPONIBLE o INACTIVO; EN_RUTA lo asigna el sistema
 * @param password             opcional: si viene, reemplaza la contraseña
 * @param activo               opcional: {@code false} desactiva la cuenta y cierra sus sesiones
 */
public record ConductorActualizarRequest(
        @NotBlank @Size(max = 100) String nombres,
        @NotBlank @Size(max = 100) String apellidos,
        @NotBlank @Pattern(regexp = Telefono.PATRON, message = "debe ser un celular peruano (9 dígitos)")
        String telefono,
        @Email @Size(max = 150) String email,
        @NotBlank @Size(max = 20) String licencia,
        Turno turno,
        EstadoDisponibilidad estadoDisponibilidad,
        @Size(min = 8, max = 72, message = "debe tener entre 8 y 72 caracteres") String password,
        Boolean activo) {
}
