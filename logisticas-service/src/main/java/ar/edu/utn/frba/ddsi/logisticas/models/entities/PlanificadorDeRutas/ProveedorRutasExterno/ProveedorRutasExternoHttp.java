package ar.edu.utn.frba.ddsi.logisticas.models.entities.PlanificadorDeRutas.ProveedorRutasExterno;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProveedorRutasExternoHttp implements ProveedorRutasExterno {

  private static final Logger log = LoggerFactory.getLogger(ProveedorRutasExternoHttp.class);

  private final HttpClient httpClient;
  private final String urlApiExterna;
  private final ObjectMapper objectMapper;

  public ProveedorRutasExternoHttp(String urlApiExterna) {
    this.httpClient = HttpClient.newHttpClient();
    this.urlApiExterna = urlApiExterna;
    this.objectMapper = new ObjectMapper();
  }

  @Override
  public void solicitarPlanificacion(List<ItemEntrega> lote, List<Camion> camionesDisponibles) {
    try {
      Map<String, Object> payload = new HashMap<>();
      payload.put("donaciones", lote);
      payload.put("camiones", camionesDisponibles);

      String jsonPayload = objectMapper.writeValueAsString(payload);

      HttpRequest request = HttpRequest.newBuilder()
                                       .uri(URI.create(urlApiExterna))
                                       .header("Content-Type", "application/json")
                                       .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                                       .build();

      httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                  if (response.statusCode() >= 400) {
                    log.error("El proveedor externo rechazó el lote ({}): {}",
                            response.statusCode(), response.body());
                  } else {
                    log.info("Lote enviado al proveedor externo");
                  }
                });

    } catch (Exception e) {
      log.error("No se pudo contactar la API externa de ruteo", e);
    }
  }
}
