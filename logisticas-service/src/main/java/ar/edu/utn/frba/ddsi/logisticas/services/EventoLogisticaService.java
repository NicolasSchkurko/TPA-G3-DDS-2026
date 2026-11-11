package ar.edu.utn.frba.ddsi.logisticas.services;


import ar.edu.utn.frba.ddsi.logisticas.dto.evento.EventoLogisticaDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.evento.EventoLogisticaResponseDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioEventoLogistica;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import java.util.List;

@Service
public class EventoLogisticaService {
  private static final int LIMITE_POR_DEFECTO = 100;
  private static final int LIMITE_MAXIMO = 500;

  private final RepositorioEventoLogistica repoEventos;

  public EventoLogisticaService(RepositorioEventoLogistica repoEventos){
    this.repoEventos = repoEventos;
  }

  /** @param desdeId ultimo id que el cliente ya procesa; {@code null} o 0 significan "desde el
    *  principio". Se pasa tal cual porque el repositorio ya devuelve {@code id > :id}: restarle
    *  uno convertiria el "estrictamente mayor" en un "mayor o igual" que reenvia el ultimo evento. */
  public EventoLogisticaResponseDTO obtenerEventosNuevos(Long desdeId) {
      return obtenerEventosNuevos(desdeId, LIMITE_POR_DEFECTO);
  }

  /** Polling acotado: como máximo {@link #LIMITE_MAXIMO} eventos por request. */
  public EventoLogisticaResponseDTO obtenerEventosNuevos(Long desdeId, Integer limite) {
      long desde = desdeId == null ? 0L : desdeId;
      int cantidad = limite == null
              ? LIMITE_POR_DEFECTO
              : Math.max(1, Math.min(limite, LIMITE_MAXIMO));
      List<EventoLogisticaDTO> eventos = repoEventos
              .findByIdGreaterThanOrderByIdAsc(desde, PageRequest.of(0, cantidad)).stream()
              .map(evento -> new EventoLogisticaDTO().aplicar(evento))
              .toList();
      return new EventoLogisticaResponseDTO(eventos);
  }
}