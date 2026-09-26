package ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories;

import ar.edu.utn.frba.ddsi.incentivos.exceptions.InexistenteException;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.Ranking;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.RankingMensual;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepositorioRankings extends JpaRepository<RankingMensual, UUID> {

    Optional<RankingMensual> findByPeriodo(YearMonth periodo);

    Optional<RankingMensual> findFirstByOrderByPeriodoDesc();

    @Query("SELECT p FROM RankingMensual rm JOIN rm.posiciones p WHERE rm.idRanking = :idRanking AND p.idUsuario = :idUsuario")
    Optional<Ranking> findPosicionEnRanking(@Param("idRanking") UUID idRanking, @Param("idUsuario") UUID idUsuario);

    default Ranking obtenerPosicionActualDeUsuario(UUID idUsuario) {
        RankingMensual rank = findFirstByOrderByPeriodoDesc()
            .orElseThrow(InexistenteException::new);

        return findPosicionEnRanking(rank.getIdRanking(), idUsuario)
            .orElse(null);
    }
}