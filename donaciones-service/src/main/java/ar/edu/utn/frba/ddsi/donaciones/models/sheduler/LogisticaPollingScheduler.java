package ar.edu.utn.frba.ddsi.donaciones.models.sheduler;

import ar.edu.utn.frba.ddsi.donaciones.config.RabbitMQConfig;
import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.SolicitudEventosDTO;
import lombok.Getter;
import lombok.Setter;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
public class LogisticaPollingScheduler {
    private final RabbitTemplate rabbitTemplate;

    @Value("${servicio.logisticas.url}")
    private String logisticasUrl;

    private Long ultimoIdProcesado = 0L;

    public LogisticaPollingScheduler(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    // Se ejecuta cada 2 minutos (120000 ms)
    @Scheduled(fixedDelay = 120000)
    public void buscarNuevosEventosLogistica() {
        System.out.println("[Polling] Buscando nuevos eventos de Logística vía RabbitMQ...");
        try {
            SolicitudEventosDTO desdeId = new SolicitudEventosDTO(ultimoIdProcesado);
            rabbitTemplate.convertAndSend(RabbitMQConfig.LOGISTICAS_EXCHANGE, RabbitMQConfig.ROUTING_KEY_SOLICITUD_EVENTOS, desdeId);
        } catch (Exception e) {
            System.err.println("Error al consumir eventos de Logística por Polling: " + e.getMessage());
        }
    }
}