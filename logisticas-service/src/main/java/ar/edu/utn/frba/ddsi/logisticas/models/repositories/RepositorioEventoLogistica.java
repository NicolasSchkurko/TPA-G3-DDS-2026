package ar.edu.utn.frba.ddsi.logisticas.models.repositories;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica.EventoLogistica;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import org.springframework.data.domain.Pageable;

@Repository
public interface RepositorioEventoLogistica extends MongoRepository<EventoLogistica, Long> {

    List<EventoLogistica> findByIdGreaterThanOrderByIdAsc(
            Long secuencia,
            Pageable pageable
    );
}