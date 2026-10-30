package ar.edu.utn.frba.ddsi.logisticas.models.repositories;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Provincia;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RepositorioProvincias extends MongoRepository<Provincia, String> {
    Optional<Provincia> findByNombreIgnoreCaseAndPais_IdPais(
            String nombre, String idPais);
}
