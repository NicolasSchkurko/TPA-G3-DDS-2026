package ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignia.Insignia;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RepositorioInsignias extends JpaRepository<Insignia, UUID> {
}
