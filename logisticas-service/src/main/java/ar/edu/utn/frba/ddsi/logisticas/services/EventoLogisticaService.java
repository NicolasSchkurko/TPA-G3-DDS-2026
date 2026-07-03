package ar.edu.utn.frba.ddsi.logisticas.services;


import ar.edu.utn.frba.ddsi.logisticas.dto.evento.PayloadEntregaDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.evento.PayloadInicioRutaDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica.EventoLogistica;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioEventoLogistica;
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

  private final RepositorioEventoLogistica repositorioEventos;
  private final ObjectMapper objectMapper;

  public EventoLogisticaService(RepositorioEventoLogistica repositorioEventos) {
    this.repositorioEventos = repositorioEventos;
    this.objectMapper = new ObjectMapper();
  }

  public List<EventoLogistica> obtenerEventosNuevos(Long desdeId) {
    return repositorioEventos.findByIdGreaterThanOrderByFechaAsc(desdeId);
  }

  public void publicarInicioRuta(Ruta ruta) {
    ruta.setUrlSeguimiento(TEMPLATE_URL_SEGUIMIENTO + ruta.getIdRuta());

    List<String> idsDonacion = ruta.obtenerTodosLosItems().stream()
                                   .map(item -> item.getIdDonacion().toString())
                                   .toList();

    PayloadInicioRutaDTO payload = new PayloadInicioRutaDTO(idsDonacion, ruta.getUrlSeguimiento());

    EventoLogistica evento = new EventoLogistica(
        "INICIO_RUTA", ruta.getIdRuta().toString(), LocalDateTime.now(), null
    );
    evento.setPayloadJson(serializar(payload));
    repositorioEventos.save(evento);
  }

  public void publicarEntregaConfirmada(ItemEntrega item, Ruta ruta) {
    EventoLogistica evento = new EventoLogistica(
        "ENTREGA_CONFIRMADA", item.getIdDonacion().toString(), LocalDateTime.now(), null
    );
    evento.setPayloadJson(serializar(payloadDatosEntrega(item, ruta)));
    repositorioEventos.save(evento);
  }

  // Adaptado para recibir la justificación separada y persistirla en el evento
  public void publicarEntregaFallida(ItemEntrega item, Ruta ruta, String justificacion) {
    EventoLogistica evento = new EventoLogistica(
        "ENTREGA_FALLIDA", item.getIdDonacion().toString(), LocalDateTime.now(), justificacion
    );
    evento.setPayloadJson(serializar(payloadDatosEntrega(item, ruta)));
    repositorioEventos.save(evento);
  }

  public void publicarReingresoDeposito(ItemEntrega item) {
    EventoLogistica evento = new EventoLogistica(
        "REINGRESO_DEPOSITO", item.getIdDonacion().toString(), LocalDateTime.now(), null
    );
    repositorioEventos.save(evento);
  }

  private PayloadEntregaDTO payloadDatosEntrega(ItemEntrega item, Ruta ruta) {
    LocalDateTime momento = item.getFechaCambioEstado();

    return new PayloadEntregaDTO(
        momento != null ? momento.format(FORMATO_FECHA) : null,
        momento != null ? momento.format(FORMATO_HORA) : null,
        ruta.getCamionAsignado() != null ? ruta.getCamionAsignado().getPatente() : null,
        nombreChofer(ruta)
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
}