package ar.edu.utn.frba.ddsi.incentivos.models.scheduler;

import ar.edu.utn.frba.ddsi.incentivos.services.RankingService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

@Component
public class RankingScheduler {
    private final RankingService  rankingService;

    public RankingScheduler(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    // Ejecuta el proceso el 1ro de cada mes a las 00:05 y genera el ranking del mes anterior
    @Scheduled(cron = "0 5 0 1 * ?")
    public void ejecutarRankingMensual() {
        YearMonth periodo = YearMonth.now().minusMonths(1);
        rankingService.generarRankingMensual(periodo);
    }
}
