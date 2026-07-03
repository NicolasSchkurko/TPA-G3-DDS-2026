package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.EventoLogisticaDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Estado;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones.GestorNotificacionesEventos;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioDonaciones;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class LogisticaEventosConsumerService {

  private final RepositorioDonaciones repositorioDonaciones;
  private final GestorNotificacionesEventos gestorNotificaciones;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  // En la vida real esto se guarda en base de datos para no perderlo al reiniciar el server
  private Long ultimoIdProcesado = 0L;
  private final String LOGISTICA_URL = "http://localhost:8080/api/logistica/eventos?desdeId=";

  public LogisticaEventosConsumerService(RepositorioDonaciones repositorioDonaciones,
                                         GestorNotificacionesEventos gestorNotificaciones) {
    this.repositorioDonaciones = repositorioDonaciones;
    this.gestorNotificaciones = gestorNotificaciones;
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
      JsonNode payload = objectMapper.readTree(evento.getPayloadJson());

      switch (evento.getTipoEvento()) {
        case "INICIO_RUTA":
          manejarInicioRuta(payload);
          break;
        case "ENTREGA_CONFIRMADA":
          manejarEntregaConfirmada(payload);
          break;
        case "ENTREGA_FALLIDA":
          manejarEntregaFallida(payload);
          break;
        case "REINGRESO_DEPOSITO":
          manejarReingresoDeposito(payload);
          break;
        default:
          System.out.println("Evento de logística desconocido: " + evento.getTipoEvento());
      }
    } catch (Exception e) {
      System.err.println("Error al procesar el payload del evento " + evento.getId() + ": " + e.getMessage());
    }
  }

  private void manejarInicioRuta(JsonNode payload) {
    // Logística nos manda una lista de IDs de donaciones
    payload.get("items").forEach(itemNode -> {
      UUID idDonacion = UUID.fromString(itemNode.asText());
      Optional<Donacion> donacionOpt = repositorioDonaciones.findById(idDonacion);

      if (donacionOpt.isPresent()) {
        Donacion donacion = donacionOpt.get();
        donacion.actualizarEstado(Estado.EN_TRASLADO, "Ruta iniciada por Logística");
        repositorioDonaciones.save(donacion);

        // Disparamos notificaciones
        gestorNotificaciones.notificarDonacionEnViajeAEntidadBeneficiaria(donacion.getEntidad().getCorreosRepresentantes());
        gestorNotificaciones.notificarDonacionEnViajeAPersonaDonante(donacion.getDonante().getMediosDeContacto());
      }
    });
  }

  private void manejarEntregaConfirmada(JsonNode payload) {
    UUID idDonacion = UUID.fromString(payload.get("idDonacion").asText());
    Optional<Donacion> donacionOpt = repositorioDonaciones.findById(idDonacion);

    if (donacionOpt.isPresent()) {
      Donacion donacion = donacionOpt.get();
      donacion.actualizarEstado(Estado.ENTREGADO, "Entrega confirmada por la entidad");
      repositorioDonaciones.save(donacion);

      gestorNotificaciones.notificarComprobanteEntregaAEntidadBeneficiaria(donacion.getEntidad().getCorreosRepresentantes());
      gestorNotificaciones.notificarComprobanteEntregaAPersonaDonante(donacion.getDonante().getMediosDeContacto());

      // TODO: Podés agregar acá la lógica para reducir la necesidad de la Entidad.
    }
  }

  private void manejarEntregaFallida(JsonNode payload) {
    UUID idDonacion = UUID.fromString(payload.get("idDonacion").asText());
    Optional<Donacion> donacionOpt = repositorioDonaciones.findById(idDonacion);

    if (donacionOpt.isPresent()) {
      Donacion donacion = donacionOpt.get();
      donacion.actualizarEstado(Estado.EN_DEPOSITO, "Falla reportada en destino");
      repositorioDonaciones.save(donacion);

      gestorNotificaciones.notificarEntregaNoRecibidaAEntidadBeneficiaria(donacion.getEntidad().getCorreosRepresentantes());
      gestorNotificaciones.notificarEntregaNoRecibidaAPersonaDonante(donacion.getDonante().getMediosDeContacto());
      gestorNotificaciones.notificarEntregaNoRecibidaAdmin(donacion.getDonante().getMediosDeContacto());
    }
  }

  private void manejarReingresoDeposito(JsonNode payload) {
    UUID idDonacion = UUID.fromString(payload.get("idDonacion").asText());
    Optional<Donacion> donacionOpt = repositorioDonaciones.findById(idDonacion);

    if (donacionOpt.isPresent()) {
      Donacion donacion = donacionOpt.get();
      // Vuelve a estar pendiente para ser asignada en la próxima corrida del Matchmaking
      donacion.actualizarEstado(Estado.PENDIENTE_ASIGNACION, "Reingresado a depósito tras revisión de entrega fallida");
      repositorioDonaciones.save(donacion);
    }
  }
}