package ar.edu.utn.frba.ddsi.logisticas.models.repositories;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica.EventoLogistica;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepositorioEventoLogistica extends MongoRepository<EventoLogistica, String> {

    /**
     * Derived query: {@code WHERE id > :id ORDER BY id ASC}. El orden lo garantiza el nombre,
     * que es lo que necesita un consumidor que hace polling por id.
     */
    List<EventoLogistica> findByIdGreaterThanOrderByIdAsc(String id);
}