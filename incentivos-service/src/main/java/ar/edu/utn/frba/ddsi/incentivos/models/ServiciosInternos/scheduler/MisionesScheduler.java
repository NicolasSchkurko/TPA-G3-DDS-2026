package ar.edu.utn.frba.ddsi.incentivos.models.ServiciosInternos.scheduler;

import ar.edu.utn.frba.ddsi.incentivos.services.PerfilService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Corre una vez por día la revisión de las misiones que exigen constancia. Es una tarea
 * programada: la constancia depende del paso del tiempo, no de un evento.
 */
@Component
public class MisionesScheduler {
    private final PerfilService service;

    public MisionesScheduler(PerfilService service) {
        this.service = service;
    }

    /**
     * Recorre los perfiles con una misión en curso y recalcula su racha. Corre a medianoche
     * para que el día que se mira sea siempre el mismo.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void evaluarProgresosConstantes() {
        service.evaluarConstanciaPerfiles();
    }
}
