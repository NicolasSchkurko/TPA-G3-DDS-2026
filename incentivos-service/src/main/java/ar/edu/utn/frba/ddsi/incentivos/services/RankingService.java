package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.PosicionRanking;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.RankingMensual;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioRankings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

@Service
public class RankingService {
    private final RepositorioRankings repo = RepositorioRankings.getInstance();

    private final PerfilService perfilService;

    public RankingService(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    // Ejecuta el proceso el día 1 de cada mes a las 00:05 y genera el ranking del mes anterior
    @Scheduled(cron = "0 5 0 1 * ?")
    public void ejecutarRankingMensual() {
        YearMonth periodo = YearMonth.now().minusMonths(1);
        perfilService.generarRankingMensual(periodo);
    }
}
