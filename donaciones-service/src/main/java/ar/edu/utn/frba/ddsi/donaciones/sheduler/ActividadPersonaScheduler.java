package ar.edu.utn.frba.ddsi.donaciones.sheduler;

import ar.edu.utn.frba.ddsi.donaciones.services.PersonaService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ActividadPersonaScheduler {
    private final PersonaService service;

    public ActividadPersonaScheduler(PersonaService service) {
        this.service = service;
    }

    @Scheduled(cron = "0 0 0 * * ?") // una vez por día
    public void revisarActividadPersonas() {
        service.revisarActividades();
    }
}
