package ar.edu.utn.frba.ddsi.donaciones.incentivos;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El transporte del RestTemplate inyectado tiene que soportar PATCH: el avance de
 * donación es {@code PATCH /api/perfiles/donacion/{idUsuario}} y la factory default
 * ({@code HttpURLConnection}) lo rechaza con "Invalid HTTP method: PATCH".
 */
public class RestTemplatePatchTest {

  private HttpServer servidor;

  @AfterEach
  void apagarServidor() {
    if (servidor != null) servidor.stop(0);
  }

  /** Server chico que responde 200 solo a PATCH; cualquier otro método, 405. */
  private HttpServer iniciarServerMock() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    AtomicReference<String> metodoRecibido = new AtomicReference<>();
    server.createContext("/api/perfiles/donacion/", exchange -> {
      metodoRecibido.set(exchange.getRequestMethod());
      exchange.sendResponseHeaders("PATCH".equals(exchange.getRequestMethod()) ? 200 : 405, -1);
      exchange.close();
    });
    server.start();
    return server;
  }

  @Test
  @DisplayName("El RestTemplate del bean de donaciones puede hacer PATCH")
  void elBeanDeDonacionesSoportaPatch() throws Exception {
    servidor = iniciarServerMock();
    int puerto = servidor.getAddress().getPort();

    // Igual que DonacionesServiceApplication.restTemplate().
    RestTemplate restTemplate = new RestTemplate(new JdkClientHttpRequestFactory(HttpClient.newHttpClient()));

    int estado = restTemplate.exchange(
            "http://localhost:" + puerto + "/api/perfiles/donacion/" + UUID.randomUUID(),
            HttpMethod.PATCH, new HttpEntity<>(null), Void.class)
        .getStatusCode().value();

    assertEquals(200, estado);
  }

  @Test
  @DisplayName("La factory default no lo soporta: por eso el 500 del I/O error")
  void laFactoryDefaultRechazaPatch() {
    RestTemplate restTemplateVieja = new RestTemplate();

    ResourceAccessException rechazado = assertThrows(ResourceAccessException.class, () ->
        restTemplateVieja.exchange("http://localhost:59999/api/perfiles/donacion/" + UUID.randomUUID(),
            HttpMethod.PATCH, new HttpEntity<>(null), Void.class));

    assertTrue(rechazado.getMessage().contains("PATCH"),
            "el I/O error tiene que señalar el método PATCH");
  }
}
