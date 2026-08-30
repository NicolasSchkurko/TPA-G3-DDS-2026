package ar.edu.utn.frba.ddsi.logisticas.models.entities.PlanificadorDeRutas.ProveedorRutasExterno;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProveedorRutasExternoHttp implements ProveedorRutasExterno {

  private final HttpClient httpClient;
  private final String urlApiExterna;
  private final ObjectMapper objectMapper;

  public ProveedorRutasExternoHttp(String urlApiExterna) {
    this.httpClient = HttpClient.newHttpClient();
    this.urlApiExterna = urlApiExterna;
    this.objectMapper = new ObjectMapper(); // Instanciamos el serializador JSON
  }

  @Override
  public void solicitarPlanificacion(List<ItemEntrega> lote, List<Camion> camionesDisponibles) {
    try {
      Map<String, Object> payloadData = new HashMap<>();
      payloadData.put("donaciones", lote);
      payloadData.put("camiones", camionesDisponibles);

      String jsonPayload = objectMapper.writeValueAsString(payloadData);

      System.out.println("JSON ENVIADO AL PROVEEDOR:");
      System.out.println(jsonPayload);

      HttpRequest request = HttpRequest.newBuilder()
                                       .uri(URI.create(urlApiExterna))
                                       .header("Content-Type", "application/json")
                                       .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                                       .build();

      httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                  if (response.statusCode() >= 400) {
                    System.err.println("Error al contactar al proveedor externo: " + response.body());
                  } else {
                    System.out.println("Lote enviado al proveedor externo exitosamente.");
                  }
                });

    } catch (Exception e) {
      System.err.println("Excepción al intentar llamar a la API externa de ruteo: " + e.getMessage());
    }
  }
}