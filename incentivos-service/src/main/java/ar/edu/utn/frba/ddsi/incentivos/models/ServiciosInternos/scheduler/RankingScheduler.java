package ar.edu.utn.frba.ddsi.incentivos.models.ServiciosInternos.scheduler;

import ar.edu.utn.frba.ddsi.incentivos.services.RankingService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Genera el ranking del mes anterior, una vez al mes. Es un snapshot inmutable: no se
 * recalcula.
 */
@Component
public class RankingScheduler {
    private final RankingService service;

    public RankingScheduler(RankingService service) {
        this.service = service;
    }

    /**
     * Cierra el ranking del mes que acaba de terminar. Corre el día 1 a las 00:05 para no
     * coincidir con {@link MisionesScheduler}.
     */
    @Scheduled(cron = "0 5 0 1 * ?")
    public void ejecutarRankingMensual() {
        service.crearRankingMensualActual();
    }
}
