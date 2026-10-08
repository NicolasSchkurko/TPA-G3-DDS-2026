package ar.edu.utn.frba.ddsi.logisticas.services;


import ar.edu.utn.frba.ddsi.logisticas.dto.evento.EventoLogisticaDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.evento.EventoLogisticaResponseDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica.EventoLogistica;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioEventoLogistica;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EventoLogisticaService {
  private final RepositorioEventoLogistica repoEventos;

  public EventoLogisticaService(RepositorioEventoLogistica repoEventos){
    this.repoEventos = repoEventos;
  }

  public EventoLogisticaResponseDTO obtenerEventosNuevos(Long desdeId) {
    return new EventoLogisticaResponseDTO(convertirEventosADTO(repoEventos.findByIdEventoGreaterThanOrderByIdEventoAsc((desdeId - 1))));
  }

  private List<EventoLogisticaDTO> convertirEventosADTO(List<EventoLogistica> eventos){
    return eventos.stream().map(this::convertirAEventoDTO).toList();
  }

  private EventoLogisticaDTO convertirAEventoDTO(EventoLogistica evento){
    return new EventoLogisticaDTO(evento.getIdEvento(), evento.getTipoEvento(), evento.getReferenciaId(), evento.getJustificacion(), evento.getPayloadJson());
  }
}