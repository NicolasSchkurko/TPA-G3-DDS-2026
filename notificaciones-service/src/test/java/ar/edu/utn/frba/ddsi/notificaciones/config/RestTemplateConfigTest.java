package ar.edu.utn.frba.ddsi.notificaciones.config;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Un servidor local que acepta y no responde: la llamada tiene que cortar por timeout. */
class RestTemplateConfigTest {

    @Test
    void unN8nQueNoRespondeCortaPorTimeoutEnVezDeColarElHilo() throws Exception {
        RestTemplateConfig config = new RestTemplateConfig();
        // Valores chicos para que el test sea rápido; el mecanismo es el mismo que en producción.
        ReflectionTestUtils.setField(config, "connectTimeoutMs", 1000);
        ReflectionTestUtils.setField(config, "readTimeoutMs", 200);

        HttpServer servidorQueNoResponde = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidorQueNoResponde.createContext("/lento", exchange -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        servidorQueNoResponde.start();

        try {
            RestTemplate restTemplate = config.restTemplate();
            String url = "http://localhost:" + servidorQueNoResponde.getAddress().getPort() + "/lento";

            ResourceAccessException excepcion = assertThrows(ResourceAccessException.class,
                    () -> restTemplate.getForEntity(url, String.class));

            assertInstanceOf(SocketTimeoutException.class, excepcion.getCause(),
                    "el corte tiene que ser por timeout, no por otro error de red");
        } finally {
            servidorQueNoResponde.stop(0);
        }
    }

    @Test
    void unTimeoutEnCeroSeRechazaPorqueSeríaInfinito() {
        RestTemplateConfig config = new RestTemplateConfig();
        ReflectionTestUtils.setField(config, "connectTimeoutMs", 5000);
        ReflectionTestUtils.setField(config, "readTimeoutMs", 0);

        assertThrows(IllegalStateException.class, config::restTemplate);
    }
}
