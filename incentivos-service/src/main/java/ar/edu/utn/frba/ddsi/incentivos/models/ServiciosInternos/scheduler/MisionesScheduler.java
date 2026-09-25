package ar.edu.utn.frba.ddsi.incentivos.models.ServiciosInternos.scheduler;

import ar.edu.utn.frba.ddsi.incentivos.services.PerfilService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MisionesScheduler {
    private final PerfilService service;

    public MisionesScheduler(PerfilService service) {
        this.service = service;
    }

    // 1 vez por dia se revisan los perfiles para actualizarles el
    // progreso en las misiones que requieren constancia
    @Scheduled(cron = "0 0 0 * * ?")
    public void evaluarProgresosConstantes() {
        service.evaluarConstanciaPerfiles();
    }
}
