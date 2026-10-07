package ar.edu.utn.frba.ddsi.incentivos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Arranque del servicio de incentivos.
 *
 * <p>{@code @EnableScheduling} activa los schedulers de constancia de misiones, ranking
 * mensual y reintentos de publicaciones a n8n.
 */
@SpringBootApplication
@EnableScheduling
public class IncentivosApplication {

    /**
     * Punto de entrada de Spring Boot. No lleva javadoc propio porque no hace nada que no
     * haga el framework: solo levanta el contexto.
     */
    public static void main(String[] args) {
        SpringApplication.run(IncentivosApplication.class, args);
    }

    // El RestTemplate se define en HttpClientConfig, con timeouts explicitos. Antes vivia
    // aca como un `new RestTemplate()` pelado, que usa timeouts infinitos: ver el punto 12.
}
