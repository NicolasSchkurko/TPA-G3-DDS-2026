package ar.edu.utn.frba.ddsi.donaciones;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import java.net.http.HttpClient;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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

    /**
     * Pool de las importaciones CSV: 2 concurrentes y cola acotada. Cola acotada a propósito:
     * la importación es pesada (parseo + alta con llamada HTTP por fila) y encolarla sin límite
     * solo cambia el colapso de lugar — con la cola llena se rechaza con 429 y se reintenta.
     * {@code destroyMethod} cierra el pool en el shutdown del contexto y los hilos son daemon
     * para que la JVM nunca quede esperándolos.
     */
    @Bean(destroyMethod = "shutdown")
    public ExecutorService executorImportacion() {
        AtomicInteger numeroDeHilo = new AtomicInteger();
        return new ThreadPoolExecutor(
                2, 2, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(8),
                tarea -> {
                    Thread hilo = new Thread(tarea, "importacion-csv-" + numeroDeHilo.incrementAndGet());
                    hilo.setDaemon(true);
                    return hilo;
                });
    }
}