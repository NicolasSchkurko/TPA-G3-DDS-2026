package ar.edu.utn.frba.ddsi.donaciones.sheduler;
import ar.edu.utn.frba.ddsi.donaciones.services.EntregasService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EntregasScheduler {

    private final EntregasService entregaService;

    public EntregasScheduler(EntregasService entregaService) {
        this.entregaService = entregaService;
    }

    @Scheduled(cron = "0 0 18,0,2,4,6,8 * * *")
    public void conseguirInfoRutas() {
        entregaService.conseguirInfoAsignacionRutas();
    }
}
