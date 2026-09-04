package ar.edu.utn.frba.ddsi.incentivos.models.repositories;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Graficos.HistorialActividad;
import java.util.List;
import org.springdoc.core.converters.models.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepositorioActividades
        extends JpaRepository<HistorialActividad, UUID> {

    // Si buscas por el campo 'idPerfil' en la entidad:
    Optional<HistorialActividad> findByIdPerfil(UUID idPerfil);

    // Si esperas múltiples resultados:
    List<HistorialActividad> findAllByIdPerfil(UUID idPerfil);

    // Ejemplo con Pageable para resultados paginados:
    Page<HistorialActividad> findAllByIdPerfil(UUID idPerfil, Pageable pageable);
}