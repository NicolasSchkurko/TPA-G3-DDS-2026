package ar.edu.utn.frba.ddsi.logisticas.models.repositories;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import org.springframework.stereotype.Repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepositorioCamiones extends MongoRepository<Camion, String> {

    Optional<Camion> findByChofer_IdChofer(UUID idChofer);
}