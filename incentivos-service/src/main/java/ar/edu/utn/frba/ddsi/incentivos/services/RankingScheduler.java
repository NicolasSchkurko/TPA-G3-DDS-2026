package ar.edu.utn.frba.ddsi.incentivos.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

@Component
public class RankingScheduler {
    private final PerfilService perfilService;

    @Autowired
    public RankingScheduler(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    // Ejecuta el proceso el día 1 de cada mes a las 00:05 y genera el ranking del mes anterior
    @Scheduled(cron = "0 5 0 1 * ?")
    public void ejecutarRankingMensual() {
        YearMonth periodo = YearMonth.now().minusMonths(1);
        perfilService.generarRankingMensualCierre(periodo);
    }
}
