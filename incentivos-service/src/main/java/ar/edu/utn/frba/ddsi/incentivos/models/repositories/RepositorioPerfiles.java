package ar.edu.utn.frba.ddsi.incentivos.models.repositories;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import java.util.List;
import org.springdoc.core.converters.models.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepositorioPerfiles
        extends JpaRepository<Perfil, UUID> {

    Optional<Perfil> findByIdUsuario(UUID idUsuario);

    Optional<Perfil> findByNombreUsuario(String nombreUsuario);

    boolean existsByIdUsuario(UUID idUsuario);

    List<Perfil> findAllByCategoriaActual(Categoria categoria);

    Page<Perfil> findAll(Pageable pageable);                   // paginación

}