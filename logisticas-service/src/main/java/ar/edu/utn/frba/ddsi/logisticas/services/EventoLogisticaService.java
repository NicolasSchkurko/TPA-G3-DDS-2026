package ar.edu.utn.frba.ddsi.logisticas.services;


import ar.edu.utn.frba.ddsi.logisticas.dto.evento.EventoDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.evento.EventosDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica.EventoLogistica;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorEventos;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EventoLogisticaService {
  private final GestorEventos gestorEventos;

  public EventoLogisticaService(GestorEventos gestorEventos){
    this.gestorEventos = gestorEventos;
  }

  public EventosDTO obtenerEventosNuevos(Long desdeId) {
    return new EventosDTO(convertirEventosADTO(gestorEventos.buscarEventos(desdeId)));
  }

  private List<EventoDTO> convertirEventosADTO(List<EventoLogistica> eventos){
    return eventos.stream().map(this::convertirAEventoDTO).toList();
  }

  private EventoDTO convertirAEventoDTO(EventoLogistica evento){
    return new EventoDTO(evento.getId(), evento.getTipoEvento(), evento.getFecha(), evento.getReferenciaId(), evento.getJustificacion(), evento.getPayloadJson());
  }
}