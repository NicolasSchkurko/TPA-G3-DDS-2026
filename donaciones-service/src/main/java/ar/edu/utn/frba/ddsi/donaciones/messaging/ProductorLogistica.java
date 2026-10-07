package ar.edu.utn.frba.ddsi.donaciones.messaging;

import ar.edu.utn.frba.ddsi.donaciones.config.RabbitMQConfig;
import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.entrega.EntregaDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;

/**
 * Publica las donaciones asignadas para que logisticas las planifique.
 *
 * <p><b>Esta es la integración que el enunciado pide con broker.</b> Dice textualmente que la
 * integración entre el servicio de donaciones y el de logística debe hacerse a través de un
 * broker, y que ese broker debe permitir seleccionar entre más de un servicio de logística
 * disponible.
 *
 * <p><b>Publica al exchange, no a una cola.</b> Con la cola suelta, este módulo tenía que
 * declarar la cola que logística consume, y la declaración quedaba de este lado: si logística
 * se levantaba con otro nombre de cola, el mensaje se perdía en silencio.
 *
 * <p><b>El encabezado de partición es lo que da orden.</b> El exchange de logística es
 * consistent-hash y este encabezado es su clave: el broker manda el mensaje a una cola según su
 * hash, así que **todos los mensajes de la misma donación caen en la misma cola y los procesa
 * la misma instancia, en orden**. Sin el encabezado, con N instancias compitiendo por una sola
 * cola, dos mensajes de la misma donación pueden terminar en dos instancias a la vez y
 * procesarse al revés. Es el punto 23 del backlog de logisticas-service.
 *
 * <p><b>Por qué se manda el menor id y no el primero de la lista.</b> El mensaje lleva un id de
 * donación por bien. Si se mandara el primero, dos mensajes que compartieran ese primer id
 * pero tuvieran el resto distinto caerían en la misma cola sin ser la misma donación. El menor
 * de todos es estable para el mismo conjunto, que es lo que hay que particionar.
 *
 * <p><b>El fallo de publicación se relanza.</b> Si no se publica, la donación queda asignada en
 * la base y nadie la entrega nunca. Es el único punto donde perder el mensaje significa perder
 * trabajo humano, así que el error sube y la transacción que llama a esto se revierte.
 */
@Component
public class ProductorLogistica {

    private static final Logger log = LoggerFactory.getLogger(ProductorLogistica.class);

    /** Se usa cuando el mensaje no trae ids: mejor una sola cola que una cualquiera. */
    private static final String CLAVE_SIN_DONACION = "sin-donacion";

    private final RabbitTemplate rabbitTemplate;

    public ProductorLogistica(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Publica la donación asignada para que logisticas la planifique.
     *
     * @throws org.springframework.amqp.AmqpException si el broker no acepta el mensaje
     */
    public void publicarDonacionAsignada(EntregaDTO entrega) {
        String particion = claveDeParticion(entrega);

        // Al exchange del hash, NO al topic. Este es el que reparte por shard y es el unico que
        // mira el encabezado de particion. Publicar al topic deja el mensaje en el binding viejo
        // de la cola unica, que ya no tiene consumidor: la donacion se pierde en silencio y no
        // hay error ni warning en ningun lado.
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_INTEGRACION_HASH,
                RabbitMQConfig.RK_NUEVA_DONACION,
                entrega,
                mensaje -> {
                    // El exchange es consistent-hash sobre este encabezado: sin el encabezado
                    // puesto, el broker no puede elegir la cola y el mensaje se descarta sin ruta.
                    mensaje.getMessageProperties()
                            .setHeader(RabbitMQConfig.HEADER_PARTICION, particion);
                    return mensaje;
                }
        );

        log.debug("Donación publicada para logisticas al exchange {} con la clave {} y la "
                        + "partición {}", RabbitMQConfig.EXCHANGE_INTEGRACION_HASH,
                RabbitMQConfig.RK_NUEVA_DONACION, particion);
    }

    /**
     * Clave de partición del mensaje: el menor de los ids de donación que trae.
     *
     * <p>Es estable para el mismo conjunto de donaciones, que es lo que hay que particionar. Y
     * si el contrato llegara a ser un mensaje por donación, este método pasa a particionar por
     * esa donación sin tocar nada más.
     */
    private String claveDeParticion(EntregaDTO entrega) {
        if (entrega == null
                || entrega.getDonacionResumen() == null
                || entrega.getDonacionResumen().getIdsDonaciones() == null
                || entrega.getDonacionResumen().getIdsDonaciones().isEmpty()) {
            return CLAVE_SIN_DONACION;
        }

        return entrega.getDonacionResumen().getIdsDonaciones().stream()
                .filter(Objects::nonNull)
                .min(Comparator.comparing(UUID::toString))
                .map(UUID::toString)
                .orElse(CLAVE_SIN_DONACION);
    }
}
