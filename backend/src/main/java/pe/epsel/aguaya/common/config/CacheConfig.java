package pe.epsel.aguaya.common.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Caché Caffeine en memoria (configurada en {@code spring.cache.*}). Solo se cachean respuestas DTO
 * de consultas agregadas, nunca entidades JPA ni datos de una persona.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String TRANSPARENCIA_RESUMEN = "transparencia-resumen";
    public static final String TRANSPARENCIA_SECTORES = "transparencia-sectores";
    public static final String TRANSPARENCIA_CRONOGRAMA = "transparencia-cronograma";
}
