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

  /** @param desdeId ultimo id que el cliente ya procesa; {@code null} o 0 significan "desde el
    *  principio". Se pasa tal cual porque el repositorio ya devuelve {@code id > :id}: restarle
    *  uno convertiria el "estrictamente mayor" en un "mayor o igual" que reenvia el ultimo evento. */
  public EventoLogisticaResponseDTO obtenerEventosNuevos(Long desdeId) {
      long desde = desdeId == null ? 0L : desdeId;
      return new EventoLogisticaResponseDTO(convertirEventosADTO(repoEventos.findByIdGreaterThanOrderByIdAsc((desdeId - 1))));
  }

  private List<EventoLogisticaDTO> convertirEventosADTO(List<EventoLogistica> eventos){
    return eventos.stream().map(this::convertirAEventoDTO).toList();
  }

  private EventoLogisticaDTO convertirAEventoDTO(EventoLogistica evento){
    return new EventoLogisticaDTO(evento.getId(), evento.getTipoEvento(), evento.getReferenciaId(), evento.getJustificacion(), evento.getPayloadJson());
  }
}