package ar.edu.utn.frba.ddsi.incentivos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class HttpClientConfig {

    /**
     * El {@code RestTemplate} que usa el servicio para llamar a {@code donaciones-service},
     * {@code notificaciones-service} y n8n.
     *
     * <p><b>Los timeouts no son opcionales</b> (punto 12). Un {@code new RestTemplate()}
     * pelado usa timeouts <b>infinitos</b>: si un servicio caido no devuelve error sino que
     * simplemente no responde, el hilo queda bloqueado para siempre. Y como varias de estas
     * llamadas se hacen <b>dentro de transacciones de base de datos</b>, mientras la llamada
     * esta bloqueada la transaccion sigue abierta y retiene una conexion del pool de Hikari.
     * Con 10 conexiones (el default) y 10 requests colgados, el servicio entero deja de
     * responder consultas aunque la base de datos este perfectamente sana.
     *
     * <p>Con timeouts el peor caso pasa de "infinito" a "conexion retenida 8 segundos", que
     * es acotado y el pool se recupera solo.
     *
     * <p>Los valores son configurables por properties para poder subirlos sin recompilar.
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