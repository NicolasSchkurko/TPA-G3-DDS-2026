package ar.edu.utn.frba.ddsi.incentivos.models.repositories;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.RankingMensual;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class RepositorioRankings {
    private final List<RankingMensual> rankings;

    public RepositorioRankings() {
        this.rankings = new ArrayList<>();
    }

    public void guardar(RankingMensual nuevoRanking) {
        if (nuevoRanking == null || nuevoRanking.getPeriodo() == null) {
            return;
        }
        this.rankings.removeIf(r -> r.getPeriodo().equals(nuevoRanking.getPeriodo()));
        this.rankings.add(nuevoRanking);
    }

    public RankingMensual buscarPorPeriodo(YearMonth periodo) {
        if (periodo == null) {
            return null;
        }
        return this.rankings.stream()
                .filter(r -> r.getPeriodo().equals(periodo))
                .findFirst()
                .orElse(null);
    }

    public RankingMensual buscarPorId(UUID idRanking){
        if(idRanking == null){
            return null;
        }
        return rankings.stream()
                .filter(r -> idRanking.equals(r.getIdRanking()))
                .findFirst()
                .orElse(null);
    }

    public List<RankingMensual> obtenerTodos() {
        return new ArrayList<>(this.rankings);
    }
}
