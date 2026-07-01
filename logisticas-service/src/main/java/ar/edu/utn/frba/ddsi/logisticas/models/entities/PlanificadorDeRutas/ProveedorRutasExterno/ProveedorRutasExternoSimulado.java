package ar.edu.utn.frba.ddsi.logisticas.models.entities.PlanificadorDeRutas.ProveedorRutasExterno;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ProveedorRutasExternoSimulado implements ProveedorRutasExterno {

  private final String URL_CALLBACK_LOCAL = "http://localhost:8080/api/logistica/rutas/callback";
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;

  public ProveedorRutasExternoSimulado() {
    this.httpClient = HttpClient.newHttpClient();
    this.objectMapper = new ObjectMapper();
  }

  @Override
  public void solicitarPlanificacion(List<ItemEntrega> lote, List<Camion> camionesDisponibles) {
    CompletableFuture.runAsync(() -> {
      try {
        Thread.sleep(2000); // Simulamos procesamiento externo

        Map<String, List<UUID>> asignacionFinal = procesarAgrupacion(lote, camionesDisponibles);

        // Usamos Jackson para la serialización
        String jsonBody = objectMapper.writeValueAsString(asignacionFinal);

        HttpRequest request = HttpRequest.newBuilder()
                                         .uri(URI.create(URL_CALLBACK_LOCAL))
                                         .header("Content-Type", "application/json")
                                         .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                                         .build();

        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      } catch (Exception e) {
        System.err.println("Error en la simulación: " + e.getMessage());
      }
    });
  }

  private Map<String, List<UUID>> procesarAgrupacion(List<ItemEntrega> lote, List<Camion> camionesDisponibles) {
    Map<String, List<UUID>> asignacion = new HashMap<>();

    for (Camion c : camionesDisponibles) {
      asignacion.put(c.getPatente(), new ArrayList<>());
      c.resetearCargaOcupada();
    }

    Map<String, List<ItemEntrega>> itemsPorCiudad = lote.stream()
                                                        .collect(Collectors.groupingBy(item ->
                                                                                           item.getEntidadDestino().getDireccionDestino().getCiudad().getNombre()));

    for (String ciudad : itemsPorCiudad.keySet()) {
      for (ItemEntrega item : itemsPorCiudad.get(ciudad)) {

        boolean asignado = false;

        // 1. Intentar asignar a camión que ya esté yendo a esa ciudad
        for (Camion c : camionesDisponibles) {
          if (ciudad.equals(c.getCiudadDestinoActual()) && c.puedeCargar(item)) {
            c.cargar(item, ciudad);
            asignacion.get(c.getPatente()).add(item.getIdDonacion());
            asignado = true;
            break;
          }
        }

        // 2. Intentar asignar a camión vacío si no se pudo antes
        if (!asignado) {
          for (Camion c : camionesDisponibles) {
            if (c.estaVacio() && c.puedeCargar(item)) {
              c.cargar(item, ciudad);
              asignacion.get(c.getPatente()).add(item.getIdDonacion());
              asignado = true;
              break;
            }
          }
        }
      }
    }

    asignacion.entrySet().removeIf(e -> e.getValue().isEmpty());
    return asignacion;
  }
}