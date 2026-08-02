package ar.edu.utn.frba.ddsi.incentivos.models.scheduler;

import ar.edu.utn.frba.ddsi.incentivos.models.gestores.GestorPerfiles;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MisionesScheduler {
    private final GestorPerfiles serv;

    public MisionesScheduler(GestorPerfiles serv) {
        this.serv = serv;
    }

    // 1 vez por dia se revisan los perfiles para actualizarles el
    // progreso en las misiones que requieren constancia
    @Scheduled(cron = "0 0 0 * * ?")
    public void actualizarMisiones() {
        serv.verificarProgresos();
    }
}
