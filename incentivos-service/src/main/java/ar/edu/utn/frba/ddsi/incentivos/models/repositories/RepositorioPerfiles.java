package ar.edu.utn.frba.ddsi.incentivos.models.repositories;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
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
public interface RepositorioPerfiles
        extends JpaRepository<Perfil, UUID> {

    Optional<Perfil> findByIdUsuario(UUID idUsuario);

    @Query("SELECT DISTINCT i FROM Perfil p JOIN p.insignias i WHERE p.idUsuario = :idUsuario")
    List<Insignia> obtenerInsigniasPorIdUsuario(
            @Param("idUsuario") UUID idUsuario);

    @Query("SELECT pm.mision FROM Perfil p JOIN p.progresoMisionActual pm WHERE p.idUsuario = :idUsuario")
    Optional<Mision> obtenerMisionPorIdUsuario(
            @Param("idUsuario") UUID idUsuario);

    boolean existsByIdUsuario(UUID idUsuario);

    Optional<Perfil> findByNombreUsuario(String nombreUsuario);

    List<Perfil> findAllByCategoriaActual(Categoria categoria);

    Page<Perfil> findAll(Pageable pageable); // paginación

}