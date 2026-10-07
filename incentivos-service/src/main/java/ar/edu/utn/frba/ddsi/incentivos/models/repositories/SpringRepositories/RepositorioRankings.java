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
 * Consultas sobre los rankings publicados. "El ranking actual" es el último mes cerrado, no
 * el de período más alto: un ranking del mes en curso sale vacío.
 */
@Repository
public interface RepositorioRankings extends JpaRepository<RankingMensual, UUID> {

    Optional<RankingMensual> findByPeriodo(YearMonth periodo);

    /**
     * El historial paginado, con las posiciones ya cargadas para evitar una consulta por
     * ranking.
     */
    @EntityGraph(attributePaths = "posiciones")
    Page<RankingMensual> findAllByOrderByPeriodoDesc(Pageable pageable);

    /**
     * El ranking del último período ya cerrado.
     *
     * @param ultimoPeriodoValido el mes en curso: solo cuenta lo anterior
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
