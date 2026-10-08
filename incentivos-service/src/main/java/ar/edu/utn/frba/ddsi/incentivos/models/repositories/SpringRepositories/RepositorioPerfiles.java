package ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignia.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.InsigniaObtenida;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.ProgresoMision;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Consultas sobre los perfiles de donante. Incluye los {@code countBy...} que usa el borrado
 * para decidir si es seguro.
 */
@Repository
public interface RepositorioPerfiles extends JpaRepository<Perfil, UUID> {

    /** El perfil de un donante, con las relaciones que necesita el DTO en una sola consulta. */
    @Override
    @EntityGraph(attributePaths = {
            "categoriaActual",
            "insigniasObtenidas",
            "insigniasObtenidas.insignia",
            "progresoMisionActual.mision"
    })
    Optional<Perfil> findById(UUID idUsuario);

    /**
     * Progreso de la misión vigente. Devuelve el {@link ProgresoMision} entero porque el
     * avance solo está ahí.
     */
    @Query("SELECT pm FROM Perfil p JOIN p.progresoMisionActual pm WHERE p.idUsuario = :idUsuario")
    Optional<ProgresoMision> obtenerProgresoMisionPorIdUsuario(@Param("idUsuario") UUID idUsuario);

    // Conteos para decidir si un borrado es seguro y poder responder 409 en vez de un 500.

    /** Donantes que tienen esta categoría como categoría actual. */
    long countByCategoriaActual(Categoria categoria);

    /** Donantes que están actualmente haciendo esta misión. */
    long countByProgresoMisionActualMision(Mision mision);

    /** Donantes que ya obtuvieron la insignia de la misión. */
    long countByInsigniasObtenidasInsignia(Insignia insignia);

    List<Perfil> findAllByCategoriaActual(Categoria categoria);

    /** Donantes cuya misión vigente es la indicada. */
    @Query("SELECT p "
            + "FROM Perfil p "
            + "JOIN p.progresoMisionActual pm "
            + "WHERE pm.mision.idMision = :idMision")
    List<Perfil> findAllByMisionActual(@Param("idMision") UUID idMision);

    /**
 * Los perfiles cuya misión vigente exige constancia, con la misión y su regla ya cargadas
 * en la misma consulta para evitar el N+1 del scheduler.
 */
@Query("SELECT p "
        + "FROM Perfil p "
        + "JOIN FETCH p.progresoMisionActual pm "
        + "JOIN FETCH pm.mision m "
        + "JOIN FETCH m.reglaDeProgreso "
        + "WHERE m.reglaDeProgreso.constancia IS NOT NULL")
    Page<Perfil> buscarPerfilesConMisionQueRequiereConstancia(Pageable pageable);

    /**
 * Pagina las insignias de un perfil, con la insignia traída en la misma consulta. Devuelve
 * {@link InsigniaObtenida} para poder ordenar por fecha de obtención; el {@code countQuery}
 * va aparte porque el fetch duplica filas.
 */
@Query(value = "SELECT io FROM Perfil p JOIN p.insigniasObtenidas io "
        + "WHERE p.idUsuario = :idUsuario",
        countQuery = "SELECT COUNT(io) FROM Perfil p JOIN p.insigniasObtenidas io "
                + "WHERE p.idUsuario = :idUsuario")
    @EntityGraph(attributePaths = "insignia")
    Page<InsigniaObtenida> paginaInsigniasPorIdUsuario(
            @Param("idUsuario") UUID idUsuario,
            Pageable pageable
    );

    /**
     * Ranking del período: cuenta insignias obtenidas dentro del mes. El filtro es un rango
     * semiabierto para que la base use el índice, y el corte de filas lo aplica el
     * {@link Pageable}.
     *
     * @param inicio primer instante del mes, inclusive
     * @param fin   primer instante del mes siguiente, exclusive
     */
    @Query("SELECT p, COUNT(io) as total "
            + "FROM Perfil p JOIN p.insigniasObtenidas io "
            + "WHERE io.fechaObtencion >= :inicio AND io.fechaObtencion < :fin "
            + "GROUP BY p "
            + "ORDER BY total DESC, p.nombreUsuario ASC")
    List<Object[]> calcularRankingMensual(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            Pageable pageable
    );
}
