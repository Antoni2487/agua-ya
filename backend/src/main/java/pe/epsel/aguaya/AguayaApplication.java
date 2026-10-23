package pe.epsel.aguaya;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// La autenticación es propia (JWT); no se usa el usuario en memoria por defecto de Spring
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class AguayaApplication {

    public static void main(String[] args) {
        SpringApplication.run(AguayaApplication.class, args);
    }
}
