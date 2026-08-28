package ar.edu.utn.frba.ddsi.logisticas.services;


import ar.edu.utn.frba.ddsi.logisticas.dto.evento.EventoLogisticaDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.evento.EventoLogisticaResponseDTO;
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

  public EventoLogisticaResponseDTO obtenerEventosNuevos(Long desdeId) {
    return new EventoLogisticaResponseDTO(convertirEventosADTO(gestorEventos.buscarEventos(desdeId - 1)));
  }

  private List<EventoLogisticaDTO> convertirEventosADTO(List<EventoLogistica> eventos){
    return eventos.stream().map(this::convertirAEventoDTO).toList();
  }

  private EventoLogisticaDTO convertirAEventoDTO(EventoLogistica evento){
    return new EventoLogisticaDTO(evento.getId(), evento.getTipoEvento(), evento.getReferenciaId(), evento.getJustificacion(), evento.getPayloadJson());
  }
}