package ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignia.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.InsigniaObtenida;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.ProgresoMision;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Consultas sobre los perfiles de donante.
 *
 * <p>Incluye los tres {@code countBy...} que usa el borrado en cascada para decidir si es
 * seguro: sin ellos, borrar una categoría con donantes reventaba por violación de FK y el
 * cliente recibía un 500 opaco (punto 18).
 */
@Repository
public interface RepositorioPerfiles extends JpaRepository<Perfil, UUID> {

    Optional<Perfil> findByIdUsuario(UUID idUsuario);

    /**
     * Progreso de la misión vigente de un donante. Devuelve el {@link ProgresoMision}
     * entero y no solo la {@link Mision} porque el donante necesita ver cuanto
     * lleva recorrido, que solo esta en el progreso.
     */
    @Query("SELECT pm FROM Perfil p JOIN p.progresoMisionActual pm WHERE p.idUsuario = :idUsuario")
    Optional<ProgresoMision> obtenerProgresoMisionPorIdUsuario(@Param("idUsuario") UUID idUsuario);

    boolean existsByIdUsuario(UUID idUsuario);

    // ===== Conteos para decidir si un borrado es seguro (punto 18) =====
    // Sin esto, borrar una categoría con donantes o una misión ya completada revienta por
    // violación de FK y el cliente recibe un 500 opaco. Con esto se puede contestarle un
    // 409 diciendo exactamente cuántas referencias lo bloquean.

    /** Donantes que tienen esta categoría como categoría actual. */
    long countByCategoriaActual(Categoria categoria);

    /** Donantes que están actualmente haciendo esta misión. */
    long countByProgresoMisionActualMision(Mision mision);

    /** Donantes que ya obtuvieron la insignia de la misión. */
    long countByInsigniasObtenidasInsignia(Insignia insignia);

    void deleteByIdUsuario(UUID idUsuario);

    List<Perfil> findAllByCategoriaActual(Categoria categoria);

    @Query("SELECT p "
            + "JOIN p.progresoMisionActual pm "
            + "WHERE pm.mision.idMision = :idMision")
    List<Perfil> findAllByMisionActual(@Param("idMision") UUID idMision);

    @Query("SELECT p "
            + "JOIN p.progresoMisionActual pm "
            + "JOIN pm.mision m "
            + "WHERE m.reglaDeProgreso.constancia IS NOT NULL")
    List<Perfil> buscarPerfilesConMisionQueRequiereConstancia();

    /**
     * Pagina las insignias de un perfil. Devuelve {@link InsigniaObtenida} y no la
     * insignia pelada para que se pueda ordenar por fecha de obtencion, que solo
     * existe en la entidad intermedia.
     */
    @Query(value = "SELECT io FROM Perfil p JOIN p.insigniasObtenidas io "
            + "WHERE p.idUsuario = :idUsuario",
            countQuery = "SELECT COUNT(io) FROM Perfil p JOIN p.insigniasObtenidas io "
                    + "WHERE p.idUsuario = :idUsuario")
    Page<InsigniaObtenida> paginaInsigniasPorIdUsuario(
            @Param("idUsuario") UUID idUsuario,
            Pageable pageable
    );

    /**
     * Ranking del periodo: cuenta insignias obtenidas dentro del mes.
     * El corte de filas lo aplica el {@link Pageable} recibido, no la query,
     * para que el limite se pueda pedir por parametro.
     */
    @Query("SELECT p, COUNT(io) as total "
            + "FROM Perfil p JOIN p.insigniasObtenidas io "
            + "WHERE MONTH(io.fechaObtencion) = :mes AND YEAR(io.fechaObtencion) = :anio "
            + "GROUP BY p "
            + "ORDER BY total DESC, p.nombreUsuario ASC")
    List<Object[]> calcularRankingMensual(
            @Param("mes") int mes,
            @Param("anio") int anio,
            Pageable pageable
    );
}
