package ar.edu.utn.frba.ddsi.donaciones;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import java.net.http.HttpClient;

@SpringBootApplication
@EnableScheduling
public class DonacionesServiceApplication {
    public static void main(String[] args) {
        // Carga el archivo .env (si existe) como propiedades del sistema, ANTES de que arranque Spring,
        // para que placeholders como ${DB_PASSWORD} en application.properties se puedan resolver.
        // Si no hay .env (ej. en CI/Docker, donde las variables vienen del entorno real), no falla.
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        dotenv.entries().forEach(entry -> {
            if (System.getProperty(entry.getKey()) == null
                    && System.getenv(entry.getKey()) == null) {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        });

        SpringApplication.run(DonacionesServiceApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        // La factory default (HttpURLConnection) no soporta PATCH: el avance de donación a
        // incentivos es PATCH y con ella responde con "Invalid HTTP method: PATCH". El
        // HttpClient del JDK soporta PATCH sin dependencias extra.
        return new RestTemplate(new JdkClientHttpRequestFactory(HttpClient.newHttpClient()));
    }
}