package ar.edu.utn.frba.ddsi.notificaciones.messaging;

import ar.edu.utn.frba.ddsi.notificaciones.config.rabbit.RabbitConfig;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Publica las notificaciones en el exchange para que las consuma {@link ConsumidorNotificaciones}.
 *
 * <p><b>Publica un DTO, no la entidad.</b> Antes mandaba {@code Notificacion} directo. Esa
 * entidad es una entidad JPA con una relación {@code @OneToOne} a {@code Mensaje} y un enum,
 * y además su identificador es un UUID que se regenera al deserializar del otro lado: el
 * listener recibía una notificación con un id distinto al guardado y el {@code save} pisaba
 * la fila equivocada. El DTO lleva solo lo que el consumidor necesita.
 *
 * <p><b>Publica al exchange con routing key, no a la cola por nombre.</b> Con la cola
 * suelta, cualquier otro servicio podía publicar y el mensaje terminaba en la cola de
 * notificaciones porque es la única declarada; no había forma de distinguir de qué servicio
 * venía el aviso. Con el exchange, cada productor elige su routing key y cada consumidor
 * declara a qué claves escucha.
 */
@Service
public class ProductorNotificaciones {

    private static final Logger log = LoggerFactory.getLogger(ProductorNotificaciones.class);

    private final RabbitTemplate rabbitTemplate;

    public ProductorNotificaciones(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Publica un aviso originado en un servicio de dominio.
     *
     * @param routingKey clave que decide qué consumidores lo reciben
     * @param mensaje    el payload; el tipo lo decide el productor, y el consumidor lo
     *                   interpreta segun tenga o no id
     */
    public void enviar(String routingKey, Object mensaje) {
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_NOTIFICACIONES, routingKey, mensaje);
        log.debug("Notificación publicada con routing key {}", routingKey);
    }

    /**
     * Publica la notificación recién guardada.
     *
     * <p>Se mantiene el método con la entidad porque el flujo de persistencia ya la tiene y
     * no necesita construir el DTO a mano.
     */
    public void enviar(Notificacion notificacion) {
        enviar(RabbitConfig.RK_INCENTIVO, new ConsumidorNotificaciones.AvisoNotificacion(
                notificacion.getId().toString(),
                notificacion.getTipoMedioDeContacto(),
                notificacion.getDireccionDeContacto()));
    }
}