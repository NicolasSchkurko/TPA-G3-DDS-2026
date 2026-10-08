package ar.edu.utn.frba.ddsi.notificaciones.messaging;

import ar.edu.utn.frba.ddsi.notificaciones.config.rabbit.RabbitConfig;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/** Publica avisos en el exchange de notificaciones. Manda DTOs, nunca entidades. */
@Service
public class ProductorNotificaciones {

    private static final Logger log = LoggerFactory.getLogger(ProductorNotificaciones.class);

    private final RabbitTemplate rabbitTemplate;

    public ProductorNotificaciones(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void enviar(String routingKey, Object mensaje) {
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_NOTIFICACIONES, routingKey, mensaje);
        log.debug("Notificación publicada con routing key {}", routingKey);
    }

    /** Publica el aviso de una notificación ya guardada, para que se despache. */
    public void enviar(Notificacion notificacion) {
        log.info("[PRODUCTOR] Publicando aviso de la notificación {} al exchange {} (rk={})",
                notificacion.getId(), RabbitConfig.EXCHANGE_NOTIFICACIONES, RabbitConfig.RK_INCENTIVO);
        enviar(RabbitConfig.RK_INCENTIVO, new ConsumidorNotificaciones.AvisoNotificacion(
                notificacion.getId().toString(),
                notificacion.getTipoMedioDeContacto(),
                notificacion.getDireccionDeContacto()));
    }
}
