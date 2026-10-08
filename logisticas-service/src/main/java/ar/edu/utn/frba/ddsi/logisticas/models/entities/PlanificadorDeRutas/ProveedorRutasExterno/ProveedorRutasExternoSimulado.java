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
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/** Simulador de un proveedor externo de rutas: agrupa por ciudad, "procesa" dos segundos
 *  y hace el POST de callback con la asignación camión → ids de donación. */
public class ProveedorRutasExternoSimulado implements ProveedorRutasExterno {

  private static final Logger log = LoggerFactory.getLogger(ProveedorRutasExternoSimulado.class);

  private final String URL_CALLBACK_LOCAL = "http://localhost:8086/api/PlanificacionRutas/callback";
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
        Thread.sleep(2000); // simulación del procesamiento externo

        Map<String, List<UUID>> asignacionFinal = procesarAgrupacion(lote, camionesDisponibles);
        String jsonBody = objectMapper.writeValueAsString(asignacionFinal);
        log.debug("Asignación simulada: {}", jsonBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL_CALLBACK_LOCAL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      } catch (Exception e) {
        log.error("Falló la planificación simulada", e);
      }
    });
  }

  private Map<String, List<UUID>> procesarAgrupacion(List<ItemEntrega> lote, List<Camion> camionesDisponibles) {
    Map<String, List<UUID>> asignacion = new HashMap<>();

    // Creamos copias locales con los campos reales de Camion para evitar data races
    List<Camion> camionesSimulados = camionesDisponibles.stream()
            .map(c -> {
              Camion copia = new Camion(
                      c.getChofer(),
                      c.getPatente(),
                      c.getCapacidadVolumen(),
                      c.getAltura(),
                      c.getCapacidadCarga(),
                      c.getDisponible()
              );
              copia.resetearCargaOcupada();
              asignacion.put(copia.getPatente(), new ArrayList<>());
              return copia;
            })
            .collect(Collectors.toList());

    Map<String, List<ItemEntrega>> itemsPorCiudad = lote.stream()
            .collect(Collectors.groupingBy(
                    item -> item.getEntidadDestino().getDireccionDestino().getCiudad().getNombre()));

    itemsPorCiudad.forEach((ciudad, items) ->
            items.forEach(item -> asignar(item, ciudad, camionesSimulados, asignacion)));

    asignacion.entrySet().removeIf(e -> e.getValue().isEmpty());
    return asignacion;
  }

  /** Preferencia de asignación: primero un camión que ya esté yendo a esa ciudad, si no un camión vacío. */
  private void asignar(ItemEntrega item, String ciudad, List<Camion> camionesDisponibles,
                       Map<String, List<UUID>> asignacion) {
    for (Camion candidato : camionesConRutaA(ciudad, camionesDisponibles)) {
      if (candidato.puedeCargar(item)) {
        cargarEn(candidato, item, ciudad, asignacion);
        return;
      }
    }

    for (Camion candidato : camionesDisponibles) {
      if (candidato.estaVacio() && candidato.puedeCargar(item)) {
        cargarEn(candidato, item, ciudad, asignacion);
        return;
      }
    }
  }

  private List<Camion> camionesConRutaA(String ciudad, List<Camion> camionesDisponibles) {
    return camionesDisponibles.stream()
            .filter(c -> ciudad.equals(c.getCiudadDestinoActual()))
            .toList();
  }

  private void cargarEn(Camion camion, ItemEntrega item, String ciudad, Map<String, List<UUID>> asignacion) {
    camion.cargar(item, ciudad);
    asignacion.get(camion.getPatente()).add(item.getIdDonacion());
  }
}
