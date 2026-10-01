package ar.edu.utn.frba.ddsi.logisticas.RabbitMQ;

import ar.edu.utn.frba.ddsi.logisticas.config.RabbitMQConfig;
import ar.edu.utn.frba.ddsi.logisticas.dto.evento.EventoLogisticaResponseDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.evento.SolicitudEventosDTO;
import ar.edu.utn.frba.ddsi.logisticas.services.EventoLogisticaService;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class SolicitudEventosListener {

    private final EventoLogisticaService eventoService;
    private final RabbitTemplate rabbitTemplate;

    public SolicitudEventosListener(
            EventoLogisticaService eventoService,
            RabbitTemplate rabbitTemplate
    ) {
        this.eventoService = eventoService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = RabbitMQConfig.SOLICITUD_EVENTOS_QUEUE)
    public void recibirSolicitud(SolicitudEventosDTO solicitud) {

        EventoLogisticaResponseDTO respuesta = eventoService.obtenerEventosNuevos(solicitud.getDesdeId());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.LOGISTICAS_EXCHANGE,
                RabbitMQConfig.ROUTING_KEY_RESPUESTA_EVENTOS,
                respuesta
        );
    }
}
