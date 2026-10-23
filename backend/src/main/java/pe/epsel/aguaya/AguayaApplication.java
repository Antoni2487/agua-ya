package pe.epsel.aguaya;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AguayaApplication {

    public static void main(String[] args) {
        SpringApplication.run(AguayaApplication.class, args);
    }
}
