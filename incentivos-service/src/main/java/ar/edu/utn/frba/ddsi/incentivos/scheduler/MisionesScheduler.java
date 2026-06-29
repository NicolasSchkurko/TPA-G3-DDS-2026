package ar.edu.utn.frba.ddsi.incentivos.scheduler;

import ar.edu.utn.frba.ddsi.incentivos.services.PerfilService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MisionesScheduler {
    private final PerfilService serv;

    public MisionesScheduler(PerfilService serv) {
        this.serv = serv;
    }

    // 1 vez por dia se revisan los perfiles para actualizarles el
    // progreso en las misiones que requieren constancia
    @Scheduled(cron = "0 0 0 * * ?")
    public void actualizarMisiones() {
        serv.verificarProgresos();
    }
}
