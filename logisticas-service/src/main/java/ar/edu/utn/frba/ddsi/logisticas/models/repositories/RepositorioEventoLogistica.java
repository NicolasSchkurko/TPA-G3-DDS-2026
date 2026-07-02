package ar.edu.utn.frba.ddsi.logisticas.models.repositories;


import ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica.EventoLogistica;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class RepositorioEventoLogistica {

  private final List<EventoLogistica> eventos = new ArrayList<>();
  private long idCounter = 1;

  public void save(EventoLogistica evento) {
    if (evento.getId() == null) {
      evento.setId(idCounter++);
    }
    eventos.add(evento);
  }

  public List<EventoLogistica> findByIdGreaterThanOrderByFechaAsc(Long id) {
    return eventos.stream()
                  .filter(e -> e.getId() > id)
                  // Al guardarse secuencialmente en la lista, el orden de fecha y de ID coinciden
                  .collect(Collectors.toList());
  }
}