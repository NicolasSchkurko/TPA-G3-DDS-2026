package ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.YearMonth;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class RankingMensual {
    private YearMonth periodo;
    private List<PosicionRanking> posiciones;
}
