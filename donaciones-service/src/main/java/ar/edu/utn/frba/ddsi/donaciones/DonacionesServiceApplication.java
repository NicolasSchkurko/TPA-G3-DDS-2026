package ar.edu.utn.frba.ddsi.donaciones;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import org.springframework.beans.factory.annotation.Value;
import java.net.http.HttpClient;
import java.time.Duration;
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
    public RestTemplate restTemplate(
        @Value("${http.client.connect-timeout-seconds:5}") int connectTimeoutSeconds,
        @Value("${http.client.read-timeout-seconds:10}") int readTimeoutSeconds
    ) {
        // La factory default (HttpURLConnection) no soporta PATCH: el avance de donación a
        // incentivos es PATCH y con ella responde con "Invalid HTTP method: PATCH". El
        // HttpClient del JDK soporta PATCH sin dependencias extra.
        // Punto 49: Timeouts explícitos para no colgar hilos ni listeners ante indisponibilidad.
        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
            .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));
        return new RestTemplate(requestFactory);
    }

    /**
     * Pool de las importaciones CSV: 2 concurrentes y cola acotada de 8.
     * Configurado con hilos no-daemon y apagado ordenado (graceful shutdown) que espera
     * hasta 30 segundos a que las tareas en vuelo completen antes de destruir el DataSource
     * y cerrar el contexto de la aplicación.
     */
    @Bean
    public ThreadPoolTaskExecutor taskExecutorImportacion() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(8);
        executor.setThreadNamePrefix("importacion-csv-");
        executor.setDaemon(false);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @Bean
    public ExecutorService executorImportacion(ThreadPoolTaskExecutor taskExecutorImportacion) {
        return taskExecutorImportacion.getThreadPoolExecutor();
    }
}