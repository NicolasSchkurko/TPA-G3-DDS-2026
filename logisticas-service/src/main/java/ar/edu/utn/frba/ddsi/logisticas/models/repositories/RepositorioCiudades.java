package ar.edu.utn.frba.ddsi.logisticas.models.repositories;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Ciudad;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface RepositorioCiudades extends MongoRepository<Ciudad, String> {
    Optional<Ciudad> findByNombreIgnoreCaseAndProvincia_IdProvincia(
            String nombre, String idProvincia);
}
