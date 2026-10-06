package ar.edu.utn.frba.ddsi.logisticas.RabbitMQ;

import ar.edu.utn.frba.ddsi.logisticas.dto.evento.EventoLogisticaResponseDTO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PublicadorEventosDonaciones {

    private final RabbitTemplate rabbitTemplate;

    public PublicadorEventosDonaciones(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void solicitarEnvioLogistica(EventoLogisticaResponseDTO eventoDTO) {

        rabbitTemplate.convertAndSend(
                "logisticas.exchange",
                "logisticas.eventos",
                eventoDTO
        );
    }
}
