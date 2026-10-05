package ar.edu.utn.frba.ddsi.incentivos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IncentivosApplication {

    public static void main(String[] args) {
        SpringApplication.run(IncentivosApplication.class, args);
    }

    // El RestTemplate se define en HttpClientConfig, con timeouts explicitos. Antes vivia
    // aca como un `new RestTemplate()` pelado, que usa timeouts infinitos: ver el punto 12.
}