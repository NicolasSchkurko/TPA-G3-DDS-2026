package ar.edu.utn.frba.ddsi.donaciones.sheduler;

import ar.edu.utn.frba.ddsi.donaciones.services.DonacionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AsignacionScheduler {

    private final DonacionService donacionService;

    public AsignacionScheduler(DonacionService donacionService) {
        this.donacionService = donacionService;
    }

    @Scheduled(cron = "0 0 18,0,2,4,6,8 * * *")
    public void ejecutarAsignacion() {
        donacionService.asignarDonaciones();
    }
}
