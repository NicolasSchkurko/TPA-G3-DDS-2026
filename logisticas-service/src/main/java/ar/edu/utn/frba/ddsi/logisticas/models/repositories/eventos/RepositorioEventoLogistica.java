package ar.edu.utn.frba.ddsi.logisticas.models.repositories.eventos;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica.EventoLogistica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepositorioEventoLogistica extends JpaRepository<EventoLogistica, Long> {

    /**
     * Derived query: {@code WHERE id > :id ORDER BY id ASC}. El orden lo garantiza el nombre,
     * que es lo que necesita un consumidor que hace polling por id.
     */
    List<EventoLogistica> findByIdGreaterThanOrderByIdAsc(Long id);
}