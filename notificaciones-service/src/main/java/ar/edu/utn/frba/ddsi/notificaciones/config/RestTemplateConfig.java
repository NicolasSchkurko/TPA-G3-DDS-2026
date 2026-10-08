package ar.edu.utn.frba.ddsi.notificaciones.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * El {@code RestTemplate} de n8n, con timeouts: la llamada corre en el hilo del {@code @RabbitListener}.
 */
@Configuration
public class RestTemplateConfig {

    @Value("${servicio.n8n.connect-timeout-ms:5000}")
    private int connectTimeoutMs;

    @Value("${servicio.n8n.read-timeout-ms:10000}")
    private int readTimeoutMs;

    @Bean
    public RestTemplate restTemplate() {
        if (connectTimeoutMs <= 0 || readTimeoutMs <= 0) {
            // En HttpURLConnection un timeout 0 significa "sin límite": reintroduciría el cuelgue.
            throw new IllegalStateException(
                    "Los timeouts de n8n tienen que ser positivos: connect=" + connectTimeoutMs
                            + "ms, read=" + readTimeoutMs + "ms.");
        }

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);

        return new RestTemplate(factory);
    }
}
