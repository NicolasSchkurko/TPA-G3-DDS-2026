package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.EventoLogisticaDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.PayloadEntregaDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.PayloadInicioRutaDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones.NotificacionEntregaFallidaAdminDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones.NotificacionEntregaDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones.NotificacionEntregaFallidaDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones.NotificacionViajeDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Estado;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.EstrategiaNotificacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.FabricaEstrategiasNotificacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones.TipoEventoNotificacion;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioDonaciones;
import com.fasterxml.jackson.core.type.TypeReference;
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
  private final FabricaEstrategiasNotificacion fabricaEstrategias;
  private final AdminService adminService;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  // En la vida real esto se guarda en base de datos para no perderlo al reiniciar el server
  private Long ultimoIdProcesado = 0L;
  private final String LOGISTICA_URL = "http://localhost:8080/api/logistica/eventos?desdeId=";

  public LogisticaEventosConsumerService(RepositorioDonaciones repositorioDonaciones,
                                         FabricaEstrategiasNotificacion fabricaEstrategias,
                                         AdminService adminService) {
    this.repositorioDonaciones = repositorioDonaciones;
    this.fabricaEstrategias = fabricaEstrategias;
    this.adminService = adminService;
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
      switch (evento.getTipoEvento()) {
        case "INICIO_RUTA":
          manejarInicioRuta(evento);
          break;
        case "ENTREGA_CONFIRMADA":
          manejarEntregaConfirmada(evento);
          break;
        case "ENTREGA_FALLIDA":
          manejarEntregaFallida(evento);
          break;
        case "REINGRESO_DEPOSITO":
          manejarReingresoDeposito(evento);
          break;
        default:
          System.out.println("Evento de logística desconocido: " + evento.getTipoEvento());
      }
    } catch (Exception e) {
      System.err.println("Error al procesar el evento " + evento.getId() + ": " + e.getMessage());
    }
  }

  private void manejarInicioRuta(EventoLogisticaDTO evento) {
    try {
      if (evento.getPayloadJson() == null || evento.getPayloadJson().isEmpty()) {
        System.err.println("Evento INICIO_RUTA " + evento.getId() + " sin payload, se ignora.");
        return;
      }

      PayloadInicioRutaDTO payload =
              objectMapper.readValue(evento.getPayloadJson(), PayloadInicioRutaDTO.class);

      if (payload.getItems() == null) {
        return;
      }

      EstrategiaNotificacion estrategiaViaje =
              fabricaEstrategias.obtenerEstrategia(
                      TipoEventoNotificacion.DONACION_EN_VIAJE);

      for (String idTexto : payload.getItems()) {

        UUID idDonacion = UUID.fromString(idTexto);

        repositorioDonaciones.findById(idDonacion)
                .ifPresent(donacion -> {

                  donacion.actualizarEstado(
                          Estado.EN_TRASLADO,
                          "Ruta iniciada por Logística");

                  repositorioDonaciones.save(donacion);

                  estrategiaViaje.ejecutar(
                          new NotificacionViajeDTO(
                                  payload.getUrlRuta(),
                                  donacion.getDonante().getMediosDeContacto(),
                                  donacion.getEntidad().getCorreosRepresentantes()
                          ));
                });
      }

    } catch (Exception e) {
      System.err.println("Error parseando items de la ruta: " + e.getMessage());
    }
  }

  private void manejarEntregaConfirmada(EventoLogisticaDTO evento) {

    UUID idDonacion = UUID.fromString(evento.getReferenciaId());

    repositorioDonaciones.findById(idDonacion)
            .ifPresent(donacion -> {

              donacion.actualizarEstado(
                      Estado.ENTREGADO,
                      "Entrega confirmada por la entidad");

              repositorioDonaciones.save(donacion);

              PayloadEntregaDTO payload = parsearPayloadEntrega(evento);

              fabricaEstrategias
                      .obtenerEstrategia(TipoEventoNotificacion.COMPROBANTE_ENTREGA)
                      .ejecutar(
                              new NotificacionEntregaDTO(
                                      payload,
                                      donacion.getDonante().getMediosDeContacto(),
                                      donacion.getEntidad().getCorreosRepresentantes()
                              ));
            });
  }

  private void manejarEntregaFallida(EventoLogisticaDTO evento) {

    UUID idDonacion = UUID.fromString(evento.getReferenciaId());

    repositorioDonaciones.findById(idDonacion)
            .ifPresent(donacion -> {

              String justificacion =
                      evento.getJustificacion() != null
                              ? evento.getJustificacion()
                              : "Falla reportada en destino";

              donacion.actualizarEstado(
                      Estado.EN_DEPOSITO,
                      justificacion);

              repositorioDonaciones.save(donacion);

              PayloadEntregaDTO payload =
                      parsearPayloadEntrega(evento);

              fabricaEstrategias
                      .obtenerEstrategia(TipoEventoNotificacion.ENTREGA_NO_RECIBIDA)
                      .ejecutar(
                              new NotificacionEntregaFallidaDTO(
                                      payload,
                                      donacion.getDonante().getMediosDeContacto(),
                                      donacion.getEntidad().getCorreosRepresentantes(),
                                      adminService.obtenerContactosAdministradores()
                              ));
            });
  }

  private void manejarReingresoDeposito(EventoLogisticaDTO evento) {
    UUID idDonacion = UUID.fromString(evento.getReferenciaId());
    Optional<Donacion> donacionOpt = repositorioDonaciones.findById(idDonacion);

    if (donacionOpt.isPresent()) {
      Donacion donacion = donacionOpt.get();
      donacion.actualizarEstado(Estado.PENDIENTE_ASIGNACION, "Reingresado a depósito tras revisión de entrega fallida");
      repositorioDonaciones.save(donacion);
    }
  }

  private PayloadEntregaDTO parsearPayloadEntrega(EventoLogisticaDTO evento) {
    try {
      return objectMapper.readValue(evento.getPayloadJson(), PayloadEntregaDTO.class);
    } catch (Exception e) {
      System.err.println("Error parseando datos de entrega del evento " + evento.getId() + ": " + e.getMessage());
      return new PayloadEntregaDTO();
    }
  }
}