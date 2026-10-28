package ar.edu.utn.frba.ddsi.logisticas.models.repositories;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Ciudad;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface RepositorioCiudades extends MongoRepository<Ciudad, String> {
}
