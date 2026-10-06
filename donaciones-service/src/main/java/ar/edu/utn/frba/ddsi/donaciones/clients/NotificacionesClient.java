package ar.edu.utn.frba.ddsi.donaciones.clients;

import ar.edu.utn.frba.ddsi.donaciones.config.RabbitMQConfig;
import ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones.NotificacionDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Publica las notificaciones de donaciones-service.
 *
 * <p><b>Va por Rabbit y no por HTTP, porque el enunciado lo pide así para todos.</b> Dice que
 * "la integración entre los servicios de dominio y el Servicio de Notificaciones deberá
 * realizarse de forma asíncrona, a través de una cola de mensajes". Decir "los servicios de
 * dominio" es plural: los dos. Incentivos ya publica por el exchange; este módulo también.
 *
 * <p>Antes iba por HTTP con un {@code RestTemplate}, y además apuntaba a la raíz del
 * servicio, así que cada notificación daba 404. La ruta real es
 * {@code POST /api/notificaciones}, porque el controller cuelga de
 * {@code @RequestMapping("/notificaciones")} y el servicio tiene context-path {@code /api}.
 *
 * <p><b>El fallo no corta la operación de dominio.</b> Publicar es un efecto secundario de
 * registrar una donación o asignar una ruta, y si el broker está caído la donación ya está
 * guardada. Propagar la excepción dejaría al usuario viendo un error por algo que sí se
 * guardó. El mensaje que no sale queda registrado en el log, que es el punto: que se pueda ver
 * qué se perdió.
 *
 * <p><b>El exchange lo declara este servicio</b> y notificaciones ata su cola, que es la
 * frontera elegida para que el nombre de las colas no viva en los dos lados.
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