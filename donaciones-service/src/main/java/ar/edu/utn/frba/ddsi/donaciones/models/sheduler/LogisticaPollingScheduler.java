package ar.edu.utn.frba.ddsi.donaciones.models.sheduler;

import ar.edu.utn.frba.ddsi.donaciones.RabbitMQ.EventosListener;
import ar.edu.utn.frba.ddsi.donaciones.config.RabbitMQConfig;
import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.SolicitudEventosDTO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LogisticaPollingScheduler {

    private final EventosListener eventosListener;
    private final RabbitTemplate rabbitTemplate;

    public LogisticaPollingScheduler(EventosListener eventosListener, RabbitTemplate rabbitTemplate) {
        this.eventosListener = eventosListener;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelay = 120000)
    public void buscarNuevosEventosLogistica() {
        System.out.println("[Polling] Solicitando nuevos eventos de Logística vía RabbitMQ...");
        try {
            SolicitudEventosDTO desdeId = new SolicitudEventosDTO(eventosListener.getDesdeId());
            rabbitTemplate.convertAndSend(RabbitMQConfig.LOGISTICAS_EXCHANGE, RabbitMQConfig.ROUTING_KEY_SOLICITUD_EVENTOS, desdeId);
        } catch (Exception e) {
            System.err.println("Error al solicitar eventos de Logística por RabbitMQ: " + e.getMessage());
        }
    }
}