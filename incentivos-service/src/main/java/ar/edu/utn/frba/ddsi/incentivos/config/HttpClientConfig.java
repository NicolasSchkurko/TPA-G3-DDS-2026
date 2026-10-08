package ar.edu.utn.frba.ddsi.incentivos.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/** Define el cliente HTTP saliente con timeouts explícitos, para que una dependencia caída no tumbe el servicio. */
@Configuration
public class HttpClientConfig {

    /**
     * El {@code RestTemplate} para llamar a {@code donaciones-service},
     * {@code notificaciones-service} y n8n. Los timeouts evitan que un servicio que no
     * responde bloquee el hilo y retenga una conexión del pool. Son configurables.
     */
    @Bean
    public RestTemplate restTemplate(
            RestTemplateBuilder builder,
            @Value("${clientes.http.connect-timeout-ms:3000}") long connectTimeoutMs,
            @Value("${clientes.http.read-timeout-ms:5000}") long readTimeoutMs) {

        return builder
                .setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .setReadTimeout(Duration.ofMillis(readTimeoutMs))
                .build();
    }
}