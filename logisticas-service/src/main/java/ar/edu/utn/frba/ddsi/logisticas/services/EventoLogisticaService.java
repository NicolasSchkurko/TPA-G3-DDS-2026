package ar.edu.utn.frba.ddsi.logisticas.services;


import ar.edu.utn.frba.ddsi.logisticas.dto.evento.EventoDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.evento.EventosDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.evento.PayloadEntregaDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.evento.PayloadInicioRutaDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica.EventoLogistica;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.EstadoEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorEventos;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorItemEntrega;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class EventoLogisticaService {

  private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

  // TODO: reemplazar por la URL real cuando exista un servicio de tracking/mapa.
  private static final String TEMPLATE_URL_SEGUIMIENTO = "https://donaciones-app.example.com/seguimiento/";

    private final GestorEventos gestorEventos;
    private final GestorItemEntrega gestorItems;
    private final ObjectMapper objectMapper;

  public EventoLogisticaService(GestorEventos gestorEventos, GestorItemEntrega gestorItems) {
      this.gestorEventos = gestorEventos;
      this.gestorItems = gestorItems;
      this.objectMapper = new ObjectMapper();
  }

  public EventosDTO obtenerEventosNuevos(Long desdeId) {
    return new EventosDTO(convertirEventosADTO(gestorEventos.buscarEventos(desdeId)));
  }

  public void publicarInicioRuta(Ruta ruta) {
    ruta.setUrlSeguimiento(TEMPLATE_URL_SEGUIMIENTO + ruta.getIdRuta());

    ruta.getParadas().forEach(parada ->
            parada.getItems().forEach(item -> {
              if(item.getEstado() == EstadoEntrega.PENDIENTE){
                item.getEstado().cambiarEstado(item, EstadoEntrega.EN_CAMINO, gestorItems);
              }
            })
    );

    List<String> idsDonacion = ruta.obtenerTodosLosItems().stream()
                                   .map(item -> item.getIdDonacion().toString())
                                   .toList();

    PayloadInicioRutaDTO payload = new PayloadInicioRutaDTO(idsDonacion, ruta.getUrlSeguimiento());

    EventoLogistica evento = new EventoLogistica(
        "INICIO_RUTA", ruta.getIdRuta().toString(), LocalDateTime.now(), null
    );
    evento.setPayloadJson(serializar(payload));
    gestorEventos.guardarEvento(evento);
  }

  public void publicarEntregaConfirmada(ItemEntrega item, Ruta ruta, String foto) {
    if (item.getEstado() == EstadoEntrega.EN_CAMINO) {
      item.setFotoComprobante(foto);
      item.getEstado().cambiarEstado(item, EstadoEntrega.ENTREGADA, gestorItems);
      EventoLogistica evento = new EventoLogistica(
          "ENTREGA_CONFIRMADA", item.getIdDonacion().toString(), LocalDateTime.now(), null
      );
      evento.setPayloadJson(serializar(payloadDatosEntrega(item, ruta)));
      gestorEventos.guardarEvento(evento);
    }
  }

  // Adaptado para recibir la justificación separada y persistirla en el evento
  public void publicarEntregaFallida(ItemEntrega item, Ruta ruta, String justificacion) {
    item.getEstado().cambiarEstado(item, EstadoEntrega.NO_RECIBIDA, gestorItems);
    EventoLogistica evento = new EventoLogistica(
        "ENTREGA_FALLIDA", item.getIdDonacion().toString(), LocalDateTime.now(), justificacion
    );
    evento.setPayloadJson(serializar(payloadDatosEntrega(item, ruta)));
    gestorEventos.guardarEvento(evento);
  }

  public void publicarReingresoDeposito(ItemEntrega item) {
    item.getEstado().cambiarEstado(item, EstadoEntrega.PENDIENTE, gestorItems);
    EventoLogistica evento = new EventoLogistica(
            "REINGRESO_DEPOSITO", item.getIdDonacion().toString(), LocalDateTime.now(), null
    );
    gestorEventos.guardarEvento(evento);
  }

  private PayloadEntregaDTO payloadDatosEntrega(ItemEntrega item, Ruta ruta) {
    LocalDateTime momento = item.getFechaCambioEstado();

    return new PayloadEntregaDTO(
        momento != null ? momento.format(FORMATO_FECHA) : null,
        momento != null ? momento.format(FORMATO_HORA) : null,
        ruta.getCamionAsignado() != null ? ruta.getCamionAsignado().getPatente() : null,
        this.nombreChofer(ruta)
    );
  }

  private String nombreChofer(Ruta ruta) {
    if (ruta.getCamionAsignado() == null || ruta.getCamionAsignado().getChofer() == null) {
      return null;
    }
    return ruta.getCamionAsignado().getChofer().getNombre();
  }

  private String serializar(Object payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (Exception e) {
      throw new IllegalStateException("Error serializando payload de evento de logística: " + e.getMessage(), e);
    }
  }

  private List<EventoDTO> convertirEventosADTO(List<EventoLogistica> eventos){
    return eventos.stream().map(this::convertirAEventoDTO).toList();
  }

  private EventoDTO convertirAEventoDTO(EventoLogistica evento){
    return new EventoDTO(evento.getId(), evento.getTipoEvento(), evento.getFecha(), evento.getReferenciaId(), evento.getJustificacion(), evento.getPayloadJson());
  }
}