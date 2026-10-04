package ar.edu.utn.frba.ddsi.logisticas.models.repositories;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica.EventoLogistica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public interface RepositorioEventoLogistica extends JpaRepository<EventoLogistica, Long> {
  default List<EventoLogistica> findByIdGreaterThanOrderByIdAsc(Long id) {
    return this.findAll().stream()
                  .filter(e -> e.getId() > id)
                  // Al guardarse secuencialmente en la lista, el orden de fecha y de ID coinciden
                  .collect(Collectors.toList());
  }
}