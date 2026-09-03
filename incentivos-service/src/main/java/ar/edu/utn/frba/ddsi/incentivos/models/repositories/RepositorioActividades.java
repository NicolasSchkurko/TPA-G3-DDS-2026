package ar.edu.utn.frba.ddsi.incentivos.models.repositories;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Graficos.HistorialActividad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepositorioActividades
        extends JpaRepository<HistorialActividad, UUID> {

    Optional<HistorialActividad> findByIdPerfil(UUID idPerfil);
}




