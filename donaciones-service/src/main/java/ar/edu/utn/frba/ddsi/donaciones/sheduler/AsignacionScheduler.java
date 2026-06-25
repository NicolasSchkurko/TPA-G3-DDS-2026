package ar.edu.utn.frba.ddsi.donaciones.sheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ar.edu.utn.frba.ddsi.donaciones.services.AsignacionService;

@Component
public class AsignacionScheduler {

    private final AsignacionService asignacionService;

    public AsignacionScheduler(AsignacionService asignacionService) {
        this.asignacionService = asignacionService;
    }

    @Scheduled(cron = "0 0 18,0,2,4,6,8 * * *")
    public void ejecutarAsignacion() {
        asignacionService.ejecutarAsignacion();
    }
}
