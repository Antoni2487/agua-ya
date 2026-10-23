package pe.epsel.aguaya.common.config;

import java.time.ZoneId;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Propiedades propias de AguaYa (prefijo {@code aguaya}).
 */
@Validated
@ConfigurationProperties(prefix = "aguaya")
public record AguayaProperties(
        @NotNull ZoneId zonaHoraria,
        @Valid @NotNull Cors cors,
        @Valid @NotNull Geocerca geocerca) {

    public record Cors(@NotEmpty List<String> origins) {
    }

    public record Geocerca(@Positive int radioMetros) {
    }
}
