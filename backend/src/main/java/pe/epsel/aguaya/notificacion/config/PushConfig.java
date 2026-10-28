package pe.epsel.aguaya.notificacion.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class PushConfig {

    public static final String EJECUTOR_PUSH = "ejecutorPush";

    /** Las llamadas a Expo salen de este pool para no demorar la respuesta al conductor. */
    @Bean(EJECUTOR_PUSH)
    public ThreadPoolTaskExecutor ejecutorPush() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("push-");
        return executor;
    }
}
