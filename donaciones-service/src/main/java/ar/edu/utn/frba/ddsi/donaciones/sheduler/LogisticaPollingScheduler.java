package ar.edu.utn.frba.ddsi.donaciones.sheduler;

import ar.edu.utn.frba.ddsi.donaciones.services.LogisticaEventosConsumerService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LogisticaPollingScheduler {

  private final LogisticaEventosConsumerService pollingService;

  public LogisticaPollingScheduler(LogisticaEventosConsumerService pollingService) {
    this.pollingService = pollingService;
  }

  // Se ejecuta automáticamente, por ejemplo, cada 2 minutos (120000 ms)
  @Scheduled(fixedDelay = 120000)
  public void buscarNuevosEventosLogistica() {
    System.out.println("[Polling] Buscando nuevos eventos de Logística...");
    pollingService.consumirEventosDeLogistica();
  }
}