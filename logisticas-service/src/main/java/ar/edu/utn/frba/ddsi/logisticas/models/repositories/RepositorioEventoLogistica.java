package ar.edu.utn.frba.ddsi.logisticas.models.repositories;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica.EventoLogistica;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepositorioEventoLogistica extends MongoRepository<EventoLogistica, Long> {

    List<EventoLogistica> findByIdGreaterThanOrderByIdAsc(
            Long secuencia
    );
}