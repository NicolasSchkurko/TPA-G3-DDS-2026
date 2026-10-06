package ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories;

import ar.edu.utn.frba.ddsi.incentivos.exceptions.InexistenteException;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.Ranking;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.RankingMensual;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Consultas sobre los rankings publicados.
 *
 * <p>El periodo es único: hay un solo ranking por mes, y por eso {@code periodo} lleva
 * {@code unique = true} en la entidad. Eso, junto con no exponer setters del lado
 * propietario, es lo que hace que un ranking publicado no se pueda reescribir.
 *
 * <p><b>"El ranking actual" es el último mes cerrado, no el de período más alto que
 * exista</b> (punto 32). Son la misma cosa mientras no haya rankings futuros en la base,
 * pero la diferencia importa: un ranking del mes en curso siempre sale vacío, porque
 * nadie completó el mes todavía. Si se aceptara, {@code GET /api/rankings/actual} devolvería
 * una lista vacía y {@code GET /api/rankings/{id}/puestoRanking} daría 404 para todos los
 * usuarios, aunque el ranking real exista. Por eso el filtro por período va en la consulta
 * y no solo en la validación del alta: así los rankings que ya quedaron en una base de
 * desarrollo tampoco rompen el "actual".
 */
@Repository
public interface RepositorioRankings extends JpaRepository<RankingMensual, UUID> {

    Optional<RankingMensual> findByPeriodo(YearMonth periodo);

    /**
     * El historial paginado, con las posiciones ya cargadas (punto 22).
     *
     * <p>Sin este {@code LEFT JOIN FETCH}, {@code convertirRankingMesADTO} toca
     * {@code ranking.getPosiciones()} una vez por cada elemento de la página, y como la
     * colección es LAZY eso es <b>una consulta por ranking</b>: pedir 20 rankings históricos
     * son 21 consultas. Con el {@code fetch} es una sola. El corte de página lo sigue
     * aplicando Hibernate sobre la colección y no sobre la consulta, así que el tamaño de la
     * página no cambia.
     */
    @EntityGraph(attributePaths = "posiciones")
    Page<RankingMensual> findAllByOrderByPeriodoDesc(Pageable pageable);

    /**
     * El ranking del último período ya cerrado.
     *
     * @param ultimoPeriodoValido el mes en curso: solo cuenta lo que es anterior
     */
    Optional<RankingMensual> findFirstByPeriodoLessThanOrderByPeriodoDesc(YearMonth ultimoPeriodoValido);

    @Query("SELECT p FROM RankingMensual rm JOIN rm.posiciones p WHERE rm.idRanking = :idRanking AND p.idUsuario = :idUsuario")
    Optional<Ranking> findPosicionEnRanking(@Param("idRanking") UUID idRanking, @Param("idUsuario") UUID idUsuario);

    default Ranking obtenerPosicionActualDeUsuario(UUID idUsuario) {
        RankingMensual rank = findFirstByPeriodoLessThanOrderByPeriodoDesc(YearMonth.now())
            .orElseThrow(InexistenteException::new);

        return findPosicionEnRanking(rank.getIdRanking(), idUsuario)
            .orElse(null);
    }
}
