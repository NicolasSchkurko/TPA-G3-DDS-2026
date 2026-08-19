package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.EventoLogisticaDTO;
import ar.edu.utn.frba.ddsi.donaciones.gestores.GestorLogistica;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@Service
public class LogisticaEventosConsumerService {
  private final GestorLogistica gestorLogistica;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  // En la vida real esto se guarda en base de datos para no perderlo al reiniciar el server
  private Long ultimoIdProcesado = 0L;
  private final String LOGISTICA_URL = "http://localhost:8080/api/logistica/eventos?desdeId=";

  public LogisticaEventosConsumerService(GestorLogistica gestorLogistica) {
    this.gestorLogistica = gestorLogistica;
    this.objectMapper = new ObjectMapper();
    this.httpClient = HttpClient.newHttpClient();
  }

  /**
   * Este método es invocado por el Scheduler periódicamente.
   */
  public void consumirEventosDeLogistica() {
    try {
      HttpRequest request = HttpRequest.newBuilder()
                                       .uri(URI.create(LOGISTICA_URL + ultimoIdProcesado))
                                       .GET()
                                       .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() == 200) {
        List<EventoLogisticaDTO> eventos = objectMapper.readValue(
            response.body(),
            new TypeReference<List<EventoLogisticaDTO>>() {}
        );

        for (EventoLogisticaDTO evento : eventos) {
          procesarEvento(evento);
          ultimoIdProcesado = Math.max(ultimoIdProcesado, evento.getId());
        }
      }
    } catch (Exception e) {
      System.err.println("Error al consumir eventos de Logística: " + e.getMessage());
    }
  }

  private void procesarEvento(EventoLogisticaDTO evento) {
    try {
      gestorLogistica.procesarEvento(evento);
    } catch (Exception e) {
      System.err.println("Error al procesar el evento " + evento.getId() + ": " + e.getMessage());
    }
  }
}