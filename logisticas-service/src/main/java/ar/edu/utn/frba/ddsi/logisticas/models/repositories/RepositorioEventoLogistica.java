package ar.edu.utn.frba.ddsi.logisticas.models.repositories;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica.EventoLogistica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepositorioEventoLogistica extends JpaRepository<EventoLogistica, Long> {
  List<EventoLogistica> findByIdEventoGreaterThanOrderByIdEventoAsc(Long id);
}