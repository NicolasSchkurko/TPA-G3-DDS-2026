package ar.edu.utn.frba.ddsi.donaciones.config;

import ar.edu.utn.frba.ddsi.donaciones.DonacionesServiceApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class RestTemplateConfigTest {

    @Test
    @DisplayName("Punto 49: El bean RestTemplate configura connectTimeout y readTimeout")
    void restTemplate_configuraTimeoutsCorrectamente() throws Exception {
        DonacionesServiceApplication app = new DonacionesServiceApplication();
        RestTemplate restTemplate = app.restTemplate(3, 7);

        assertNotNull(restTemplate);
        assertInstanceOf(JdkClientHttpRequestFactory.class, restTemplate.getRequestFactory());

        JdkClientHttpRequestFactory factory = (JdkClientHttpRequestFactory) restTemplate.getRequestFactory();

        // Verificar readTimeout por reflexión en JdkClientHttpRequestFactory
        Field readTimeoutField = JdkClientHttpRequestFactory.class.getDeclaredField("readTimeout");
        readTimeoutField.setAccessible(true);
        Duration readTimeout = (Duration) readTimeoutField.get(factory);
        assertEquals(Duration.ofSeconds(7), readTimeout);

        // Verificar connectTimeout en HttpClient
        Field httpClientField = JdkClientHttpRequestFactory.class.getDeclaredField("httpClient");
        httpClientField.setAccessible(true);
        HttpClient httpClient = (HttpClient) httpClientField.get(factory);
        Optional<Duration> connectTimeout = httpClient.connectTimeout();
        assertTrue(connectTimeout.isPresent());
        assertEquals(Duration.ofSeconds(3), connectTimeout.get());
    }
}
