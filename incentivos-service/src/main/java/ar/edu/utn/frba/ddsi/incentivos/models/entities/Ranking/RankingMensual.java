package ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class RankingMensual {
    private UUID idRanking;
    private YearMonth periodo;
    private List<Ranking> posiciones;

    public RankingMensual(YearMonth periodo, List<Ranking> posiciones) {
        this.idRanking = UUID.randomUUID();
        this.periodo = periodo;
        this.posiciones = posiciones;
    }
}
