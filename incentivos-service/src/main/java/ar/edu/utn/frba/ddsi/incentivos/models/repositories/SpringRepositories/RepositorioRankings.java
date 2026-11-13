package ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories;

import ar.edu.utn.frba.ddsi.incentivos.exceptions.InexistenteException;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.Ranking;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.RankingMensual;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Consultas sobre los rankings publicados. */
@Repository
public interface RepositorioRankings extends JpaRepository<RankingMensual, UUID> {

    Optional<RankingMensual> findByPeriodo(YearMonth periodo);

    /**
     * El historial de rankings paginado, ordenado por período descendente.
     * La paginación se realiza en base de datos con SQL LIMIT/OFFSET sin cargar
     * el histórico completo en memoria.
     */
    Page<RankingMensual> findAllByOrderByPeriodoDesc(Pageable pageable);

    /** Último ranking publicado hasta el mes indicado, incluyendo un snapshot del mes en curso. */
    Optional<RankingMensual> findFirstByPeriodoLessThanEqualOrderByPeriodoDesc(YearMonth periodo);

    @Query("SELECT p FROM RankingMensual rm JOIN rm.posiciones p WHERE rm.idRanking = :idRanking AND p.idUsuario = :idUsuario")
    Optional<Ranking> findPosicionEnRanking(@Param("idRanking") UUID idRanking, @Param("idUsuario") UUID idUsuario);

    /** Posiciones paginadas de un ranking, ordenadas por puesto, sin cargar la colección entera. */
    @Query("SELECT p FROM Ranking p WHERE p.rankingMensual.idRanking = :idRanking ORDER BY p.puesto ASC")
    Page<Ranking> paginaPosiciones(@Param("idRanking") UUID idRanking, Pageable pageable);

    /** Cantidad de participantes de un snapshot, sin hidratar la colección de posiciones. */
    @Query("SELECT COUNT(p) FROM Ranking p WHERE p.rankingMensual = :ranking")
    long contarPosiciones(@Param("ranking") RankingMensual ranking);

    default Ranking obtenerPosicionActualDeUsuario(UUID idUsuario) {
        RankingMensual rank = findFirstByPeriodoLessThanEqualOrderByPeriodoDesc(YearMonth.now())
            .orElseThrow(InexistenteException::new);

        return findPosicionEnRanking(rank.getIdRanking(), idUsuario)
            .orElse(null);
    }
}
