package ar.edu.utn.frba.ddsi.logisticas.messaging;

import ar.edu.utn.frba.ddsi.logisticas.config.RabbitMQConfig;
import ar.edu.utn.frba.ddsi.logisticas.dto.evento.EventoLogisticaMensajeDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica.EventoLogistica;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica los eventos de trazabilidad para que {@code donaciones-service} notifique: logística
 * publica el hecho y otro servicio notifica, que es lo único compatible con no invocar a
 * nadie desde acá.
 *
 * <p>Se publica sobre un evento ya persistido, no antes: si el consumidor recibiera el evento
 * primero, trabajaría sobre un registro que todavía no existe.
 */
@Component
public class ProductorEventosLogistica {

    private static final Logger log = LoggerFactory.getLogger(ProductorEventosLogistica.class);

    private final RabbitTemplate rabbitTemplate;

    public ProductorEventosLogistica(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicar(EventoLogistica evento) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_EVENTOS,
                RabbitMQConfig.RK_EVENTO,
                EventoLogisticaMensajeDTO.desde(evento)
        );

        log.debug("Evento {} publicado con la clave {}",
                evento.getTipoEvento(), RabbitMQConfig.RK_EVENTO);
    }
}