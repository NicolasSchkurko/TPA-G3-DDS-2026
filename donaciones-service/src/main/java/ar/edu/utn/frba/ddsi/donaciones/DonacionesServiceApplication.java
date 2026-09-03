package ar.edu.utn.frba.ddsi.donaciones;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableScheduling
public class DonacionesServiceApplication {
    public static void main(String[] args) {
        // Carga el archivo .env (si existe) como propiedades del sistema, ANTES de que arranque Spring,
        // para que placeholders como ${DB_PASSWORD} en application.properties se puedan resolver.
        // Si no hay .env (ej. en CI/Docker, donde las variables vienen del entorno real), no falla.
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        dotenv.entries().forEach(entry -> {
            if (System.getProperty(entry.getKey()) == null) {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        });

        SpringApplication.run(DonacionesServiceApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
