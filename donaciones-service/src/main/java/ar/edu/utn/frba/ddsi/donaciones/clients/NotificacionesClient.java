package ar.edu.utn.frba.ddsi.donaciones.clients;

import ar.edu.utn.frba.ddsi.donaciones.config.RabbitMQConfig;
import ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones.NotificacionDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Publica las notificaciones por Rabbit (el enunciado exige integración asíncrona por cola
 * entre los servicios de dominio y el de notificaciones).
 *
 * <p>El fallo no corta la operación de dominio: si el broker está caído la donación ya se
 * guardó, y propagar la excepción mostraría al usuario un error por algo que sí persistió.
 * El mensaje perdido queda en el log.
 */
@Service
public class NotificacionesClient {

    private static final Logger log = LoggerFactory.getLogger(NotificacionesClient.class);

    private final RabbitTemplate rabbitTemplate;

    public NotificacionesClient(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void enviarNotificacion(NotificacionDTO dto) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NOTIFICACIONES,
                    RabbitMQConfig.RK_DONACION,
                    dto);

            log.debug("Notificación publicada con la clave {}", RabbitMQConfig.RK_DONACION);
        } catch (RuntimeException error) {
            log.error("No se pudo publicar la notificación con routing key {}: {}",
                    RabbitMQConfig.RK_DONACION, error.getMessage());
        }
    }
}