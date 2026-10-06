package ar.edu.utn.frba.ddsi.donaciones.messaging;

import ar.edu.utn.frba.ddsi.donaciones.config.RabbitMQConfig;
import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.entrega.EntregaDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica las donaciones asignadas para que logística las planifique.
 *
 * <p><b>Esta es la integración que el enunciado pide con broker.</b> Dice textualmente que
 * la integración entre el servicio de donaciones-service y el de logística debe hacerse a través de un
 * broker, y que ese broker debe permitir seleccionar entre más de un servicio de logística
 * disponible. Se cumple publicando en el exchange de integración con la routing key
 * {@code donaciones.creada}: todas las instancias de logística están suscritas a la misma
 * cola y el broker reparte el trabajo.
 *
 * <p><b>Publica al exchange, no a una cola.</b> Con la cola suelta, este módulo tenía que
 * declarar la cola que logística consume, y la declaración quedaba de este lado: si logística
 * se levantaba con otro nombre de cola, el mensaje se perdía en silencio.
 *
 * <p><b>El fallo de publicación se relanza.</b> Si no se publica, la donación queda asignada
 * en la base y nadie la entrega nunca. Es el único punto donde perder el mensaje significa
 * perder trabajo humano, así que el error sube y la transacción que llama a esto se revierte.
 */
@Component
public class ProductorLogistica {

    private static final Logger log = LoggerFactory.getLogger(ProductorLogistica.class);

    private final RabbitTemplate rabbitTemplate;

    public ProductorLogistica(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Publica la donación asignada para que logística la planifique.
     *
     * @throws org.springframework.amqp.AmqpException si el broker no acepta el mensaje
     */
    public void publicarDonacionAsignada(EntregaDTO entrega) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_INTEGRACION,
                RabbitMQConfig.RK_NUEVA_DONACION,
                entrega
        );

        log.debug("Donación publicada para logística con la clave {}",
                RabbitMQConfig.RK_NUEVA_DONACION);
    }
}