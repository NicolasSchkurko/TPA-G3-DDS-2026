package ar.edu.utn.frba.ddsi.logisticas.messaging;

import ar.edu.utn.frba.ddsi.logisticas.config.RabbitMQConfig;
import ar.edu.utn.frba.ddsi.logisticas.dto.evento.EventoLogisticaMensajeDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica.EventoLogistica;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica los eventos de trazabilidad para que {@code donaciones-service} notifique.
 *
 * <p><b>Es la mitad del public/subscribe que exige el enunciado.</b> El enunciado dice que el
 * servicio de logística no debe comunicarse con el servicio de notificaciones, y tampoco
 * invocar a donaciones-service o a incentivos. Pero también exige notificar tres casos: inicio de ruta,
 * entrega realizada y entrega no satisfactoria. La única forma de cumplir las dos cosas es
 * que logística <b>publique</b> el hecho y sea otro servicio el que notifique. Acá se publica;
 * la notificación la hace {@code donaciones-service}, que es quien conoce a los donantes, las entidades
 * y los administradores.
 *
 * <p><b>Se publica después de guardar el evento en la base, no antes.</b> Si se publicara
 * primero, el consumidor puede recibir el evento y trabajar sobre un registro que todavía no
 * existe, y la trazabilidad se pierde. Al revés también hay problema: si la publicación
 * falla, el evento queda guardado y el polling de contenedor lo recupera.
 *
 * <p><b>La publicación se relanza a propósito.</b> Perder un evento de entrega es perder
 * trazabilidad de algo que una persona donante pidió saber. El servicio registra la ruta, así
 * que el evento se puede regenerar desde la base.
 */
@Component
public class ProductorEventosLogistica {

    private static final Logger log = LoggerFactory.getLogger(ProductorEventosLogistica.class);

    private final RabbitTemplate rabbitTemplate;

    public ProductorEventosLogistica(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /** Publica un evento ya persistido. */
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