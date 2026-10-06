package ar.edu.utn.frba.ddsi.notificaciones.messaging;

import ar.edu.utn.frba.ddsi.notificaciones.config.rabbit.RabbitConfig;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class ProductorNotificaciones {
    private final RabbitTemplate rabbitTemplate;

    public ProductorNotificaciones(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void enviar(Notificacion notificacion) {
        rabbitTemplate.convertAndSend(
                RabbitConfig.COLA_NOTIFICACIONES,
                notificacion
        );
    }
}
