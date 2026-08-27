package ar.edu.utn.frba.ddsi.donaciones.models.sheduler;

import ar.edu.utn.frba.ddsi.donaciones.services.DonanteService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ActividadDonanteScheduler {
    private final DonanteService service;

    public ActividadDonanteScheduler(DonanteService service) {
        this.service = service;
    }

    @Scheduled(cron = "0 0 0 * * ?") // una vez por día
    public void revisarActividadDonantes() {
        service.revisarActividades();
    }
}
