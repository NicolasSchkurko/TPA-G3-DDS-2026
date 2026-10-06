package ar.edu.utn.frba.ddsi.incentivos.models.ServiciosInternos.scheduler;

import ar.edu.utn.frba.ddsi.incentivos.services.PerfilService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Corre una vez por día la revisión de las misiones que exigen constancia.
 *
 * <p>Es una tarea programada, no un endpoint: la constancia depende de que passent los
 * meses, así que no hay ningún evento que la dispare.
 */
@Component
public class MisionesScheduler {
    private final PerfilService service;

    public MisionesScheduler(PerfilService service) {
        this.service = service;
    }

    /**
     * Recorre los perfiles con una misión en curso y recalcula la racha de meses de cada
     * uno. La media noche, para que el día que se mira sea siempre el mismo.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void evaluarProgresosConstantes() {
        service.evaluarConstanciaPerfiles();
    }
}
