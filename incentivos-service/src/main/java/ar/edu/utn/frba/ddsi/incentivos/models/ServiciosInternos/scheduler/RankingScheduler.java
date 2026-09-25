package ar.edu.utn.frba.ddsi.incentivos.models.ServiciosInternos.scheduler;

import ar.edu.utn.frba.ddsi.incentivos.services.RankingService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RankingScheduler {
    private final RankingService service;

    public RankingScheduler(RankingService service) {
        this.service = service;
    }

    // Ejecuta el proceso el 1ro de cada mes a las 00:05 y genera el ranking del mes anterior
    @Scheduled(cron = "0 5 0 1 * ?")
    public void ejecutarRankingMensual() {
        service.crearRankingMensual();
    }
}
