package ar.edu.utn.frba.ddsi.incentivos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Arranque del servicio. {@code @EnableScheduling} habilita los dos schedulers (constancia y
 * ranking mensual).
 */
@SpringBootApplication
@EnableScheduling
public class IncentivosApplication {

    /** Punto de entrada de Spring Boot. */
    public static void main(String[] args) {
        SpringApplication.run(IncentivosApplication.class, args);
    }

    // El RestTemplate se define en HttpClientConfig, con timeouts explícitos.
}
