package ar.edu.utn.frba.ddsi.incentivos.models.events;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.Ranking;
import java.util.List;

public record ResultadosRanking (List<Ranking> posiciones) {
}
