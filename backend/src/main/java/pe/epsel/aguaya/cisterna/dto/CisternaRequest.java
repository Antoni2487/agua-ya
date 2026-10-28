package pe.epsel.aguaya.cisterna.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import pe.epsel.aguaya.cisterna.entity.EstadoVehiculo;

/**
 * @param placa             formato peruano {@code ABC-123}; se guarda en mayúsculas
 * @param estadoVehiculo    si se omite, OPERATIVA al crear / se mantiene al editar
 * @param venceAutorizacion sin fecha (o vencida) la cisterna no se puede asignar a rutas
 */
public record CisternaRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9]{3}-\\d{3}$", message = "debe tener el formato ABC-123")
        String placa,
        @NotNull @Positive @Max(value = 50_000, message = "no debe superar 50000 litros") Integer capacidadLitros,
        EstadoVehiculo estadoVehiculo,
        @Size(max = 50) String nroAutorizacionSanitaria,
        LocalDate venceAutorizacion) {
}
