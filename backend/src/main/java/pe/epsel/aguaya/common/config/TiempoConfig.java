package pe.epsel.aguaya.common.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj en la zona horaria del negocio. Los servicios deben usar este bean
 * (no {@code OffsetDateTime.now()} directo) para poder fijar la hora en los tests.
 */
@Configuration
public class TiempoConfig {

    @Bean
    public Clock clock(AguayaProperties properties) {
        return Clock.system(properties.zonaHoraria());
    }
}
