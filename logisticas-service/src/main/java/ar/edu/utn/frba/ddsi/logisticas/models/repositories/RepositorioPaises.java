package ar.edu.utn.frba.ddsi.logisticas.models.repositories;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Pais;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RepositorioPaises extends MongoRepository<Pais, String> {
}
