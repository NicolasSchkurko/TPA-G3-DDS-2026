package ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories;

import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.RankingFilaDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignia.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.InsigniaObtenida;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.ProgresoMision;
import java.util.Collection;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
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
     * Perfil para el camino de donaciones: categoría y progreso, sin las insignias. El
     * EntityGraph completo sigue disponible para las lecturas que arman el DTO de perfil.
     */
    @EntityGraph(attributePaths = {
            "categoriaActual",
            "progresoMisionActual",
            "progresoMisionActual.mision",
            "progresoMisionActual.mision.reglaDeProgreso"
    })
    @Query("SELECT p FROM Perfil p WHERE p.idUsuario = :idUsuario")
    Optional<Perfil> findByIdParaDonacion(@Param("idUsuario") UUID idUsuario);

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

    /** Perfiles de una categoría paginados, para no cargar toda la categoría de una vez. */
    Page<Perfil> findByCategoriaActual(Categoria categoria, Pageable pageable);

    /** Donantes cuya misión vigente es la indicada. */
    @Query("SELECT p "
            + "FROM Perfil p "
            + "JOIN p.progresoMisionActual pm "
            + "WHERE pm.mision.idMision = :idMision")
    List<Perfil> findAllByMisionActual(@Param("idMision") UUID idMision);

    /** Igual que {@link #findAllByMisionActual}, paginado para no cargar todo de una vez. */
    @Query("SELECT p FROM Perfil p "
            + "JOIN p.progresoMisionActual pm "
            + "WHERE pm.mision.idMision = :idMision")
    Page<Perfil> findByMisionActualPaginado(@Param("idMision") UUID idMision,
                                            Pageable pageable);

    /** Ids de perfiles existentes, para no consultar uno por uno en el alta en lote. */
    @Query("SELECT p.idUsuario FROM Perfil p WHERE p.idUsuario IN :ids")
    List<UUID> buscarIdsExistentes(@Param("ids") Collection<UUID> ids);

    /**
     * Los perfiles cuya misión vigente exige constancia. La consulta no lleva {@code JOIN
     * FETCH} para que Hibernate pueda paginar en SQL; las relaciones se hidratan con el
     * {@code EntityGraph} y el {@code countQuery} va explícito.
     */
    @Query(value = "SELECT p FROM Perfil p "
            + "WHERE p.activo = true "
            + "AND p.progresoMisionActual.mision.reglaDeProgreso.constancia IS NOT NULL",
            countQuery = "SELECT COUNT(p) FROM Perfil p "
                    + "WHERE p.activo = true "
                    + "AND p.progresoMisionActual.mision.reglaDeProgreso.constancia IS NOT NULL")
    @EntityGraph(attributePaths = {
            "progresoMisionActual",
            "progresoMisionActual.mision",
            "progresoMisionActual.mision.reglaDeProgreso"
    })
    Page<Perfil> buscarPerfilesConMisionQueRequiereConstancia(Pageable pageable);

    /**
 * Pagina las insignias de un perfil, con la insignia traída en la misma consulta. Devuelve
 * {@link InsigniaObtenida} para poder ordenar por fecha de obtención; el {@code countQuery}
 * va aparte porque el fetch duplica filas.
 */
@Query(value = "SELECT io FROM Perfil p JOIN p.insigniasObtenidas io JOIN FETCH io.insignia "
        + "WHERE p.idUsuario = :idUsuario ORDER BY io.fechaObtencion DESC",
        countQuery = "SELECT COUNT(io) FROM Perfil p JOIN p.insigniasObtenidas io "
                + "WHERE p.idUsuario = :idUsuario")
    Page<InsigniaObtenida> paginaInsigniasPorIdUsuario(
            @Param("idUsuario") UUID idUsuario,
            Pageable pageable
    );

    /**
     * Ranking del período: cuenta las misiones completadas durante el mes, sin depender de la
     * categoría ni del total de donaciones. Las insignias del mes cubren finalizaciones cuyo
     * evento no haya quedado marcado en la donación; se toma el mayor de ambos conteos para no
     * duplicar misiones que sí tengan evento. El corte de filas lo aplica el {@link Pageable}.
     *
     * @param inicio primer instante del mes, inclusive
     * @param fin   primer instante del mes siguiente, exclusive
     */
    @Query("SELECT new ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.RankingFilaDTO("
            + "p.idUsuario, p.nombreUsuario, "
            + "CASE WHEN "
            + "(SELECT COUNT(d) FROM ImpactoDonacion d "
            + "WHERE d.idUsuario = p.idUsuario AND d.completMision = true "
            + "AND d.fechaEntrega >= :inicio AND d.fechaEntrega < :fin) >= "
            + "(SELECT COUNT(io) FROM InsigniaObtenida io "
            + "WHERE io.perfil = p AND io.fechaObtencion >= :inicio AND io.fechaObtencion < :fin) "
            + "THEN (SELECT COUNT(d) FROM ImpactoDonacion d "
            + "WHERE d.idUsuario = p.idUsuario AND d.completMision = true "
            + "AND d.fechaEntrega >= :inicio AND d.fechaEntrega < :fin) "
            + "ELSE (SELECT COUNT(io) FROM InsigniaObtenida io "
            + "WHERE io.perfil = p AND io.fechaObtencion >= :inicio AND io.fechaObtencion < :fin) "
            + "END) "
            + "FROM Perfil p "
            + "WHERE p.activo = true AND ("
            + "(SELECT COUNT(d) FROM ImpactoDonacion d "
            + "WHERE d.idUsuario = p.idUsuario AND d.completMision = true "
            + "AND d.fechaEntrega >= :inicio AND d.fechaEntrega < :fin) > 0 OR "
            + "(SELECT COUNT(io) FROM InsigniaObtenida io "
            + "WHERE io.perfil = p AND io.fechaObtencion >= :inicio AND io.fechaObtencion < :fin) > 0) "
            + "ORDER BY CASE WHEN "
            + "(SELECT COUNT(d) FROM ImpactoDonacion d "
            + "WHERE d.idUsuario = p.idUsuario AND d.completMision = true "
            + "AND d.fechaEntrega >= :inicio AND d.fechaEntrega < :fin) >= "
            + "(SELECT COUNT(io) FROM InsigniaObtenida io "
            + "WHERE io.perfil = p AND io.fechaObtencion >= :inicio AND io.fechaObtencion < :fin) "
            + "THEN (SELECT COUNT(d) FROM ImpactoDonacion d "
            + "WHERE d.idUsuario = p.idUsuario AND d.completMision = true "
            + "AND d.fechaEntrega >= :inicio AND d.fechaEntrega < :fin) "
            + "ELSE (SELECT COUNT(io) FROM InsigniaObtenida io "
            + "WHERE io.perfil = p AND io.fechaObtencion >= :inicio AND io.fechaObtencion < :fin) "
            + "END DESC, p.nombreUsuario ASC")
    Slice<RankingFilaDTO> calcularRankingMensual(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            Pageable pageable
    );
}
