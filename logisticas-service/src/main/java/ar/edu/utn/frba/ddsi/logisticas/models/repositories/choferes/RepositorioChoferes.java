package ar.edu.utn.frba.ddsi.logisticas.models.repositories.choferes;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RepositorioChoferes extends JpaRepository<Chofer, UUID> {
}