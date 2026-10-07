package ar.edu.utn.frba.ddsi.donaciones.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topología del broker desde el lado de {@code donaciones-service}.
 *
 * <p><b>Antes declaraba las colas y los bindings de logística, y eso estaba mal.</b> El
 * servicio publicaba en el exchange de logística pero no le tocaba definir cómo logística
 * consume: la declaración de la cola ajena quedaba en este módulo. El síntoma era que
 * logisticas-service tenía su propia copia de esos beans, idéntica salvo por dos bindings
 * atados al exchange equivocado. Con las dos declaraciones, el broker aceptaba ambas y el
 * binding bueno de este módulo tapaba el malo del otro. Levantando logística sola, las colas
 * de este módulo no existían y la cadena se cortaba.
 *
 * <p><b>La frontera quedó así:</b> este servicio publica en el exchange de integración y
 * escucha la cola de eventos que logística le deja para trazabilidad. Las colas que logística
 * consume las declara logística.
 */
@Configuration
@EnableRabbit
public class RabbitMQConfig {

    /** Exchange de integración con logisticas-service. */
    public static final String EXCHANGE_INTEGRACION = "logistica.exchange";

    /**
     * Exchange de integracion por hash, que reparte las donaciones entre las instancias.
     *
     * <p><b>Es el que hay que usar para publicar una donacion</b>, no
     * {@link #EXCHANGE_INTEGRACION}. Ese es topic y solo enruta el sondeo de
     * trazabilidad; el reparto por hash vive aca. Publicar al equivocado deja el
     * mensaje en una cola sin consumidor y se pierde en silencio.
     */
    public static final String EXCHANGE_INTEGRACION_HASH = "logistica.integracion.hash";

    /** Exchange de eventos de trazabilidad que logisticas-service publica. */
    public static final String EXCHANGE_EVENTOS = "logistica.eventos.exchange";

    /** Exchange de notificaciones. */
    public static final String EXCHANGE_NOTIFICACIONES = "notificaciones.exchange";

    /** Routing key de las donaciones nuevas que esperan planificarse. */
    public static final String RK_NUEVA_DONACION = "donaciones.creada";

    /** Routing key de la solicitud de eventos de trazabilidad. */
    public static final String RK_SOLICITUD_EVENTOS = "logistica.solicitud.eventos";

    /** Routing key de los eventos de trazabilidad que logisticas-service publica. */
    public static final String RK_EVENTO = "logistica.evento";

    /**
     * Routing key de las notificaciones de donaciones-service: alta de donante, donacion
     * asignada, entregas.
     */
    public static final String RK_DONACION = "notificaciones.donacion";

    /**
     * Encabezado con el que logística particiona el trabajo entre sus instancias.
     *
     * <p>Es la clave del exchange consistent-hash de logística: el broker manda el mensaje a
     * una cola según el hash de este encabezado, de modo que **todos los mensajes de la misma
     * donación caen siempre en la misma cola y los procesa la misma instancia, en orden**.
     *
     * <p>Sin esto, con N instancias compitiendo por una sola cola, el broker reparte los
     * mensajes de a uno en cualquier orden: dos mensajes de la misma donación pueden terminar
     * en dos instancias al mismo tiempo, y el que se procesa segundo puede ser el que se envió
     * primero. Ese es el punto 23 del backlog de logística.
     *
     * <p>El nombre del encabezado no está inventado: es el que declara
     * {@code CustomExchange} de Spring AMQP para el tipo {@code x-consistent-hash}. Es un
     * contrato entre los dos servicios, y por eso es una constante compartida en el
     * lado de quien publica.
     */
    public static final String HEADER_PARTICION = "x-id-donacion";

    /** Cola propia donde este servicio consume los eventos de trazabilidad. */
    public static final String COLA_EVENTOS = "donaciones.eventos.queue";

    /**
     * Declara solo el exchange, no la cola de entrada de logisticas-service.
     *
     * <p>Un exchange es un punto de encuentro: lo declara quien publica y lo usan todos los
     * consumidores, sin que ninguno tenga que saber el nombre de la cola del otro.
     */
    @Bean
    public TopicExchange exchangeIntegracion() {
        return new TopicExchange(EXCHANGE_INTEGRACION, true, false);
    }

    @Bean
    public TopicExchange exchangeEventos() {
        return new TopicExchange(EXCHANGE_EVENTOS, true, false);
    }

    @Bean
    public TopicExchange exchangeNotificaciones() {
        return new TopicExchange(EXCHANGE_NOTIFICACIONES, true, false);
    }

    @Bean
    public Binding bindingEventos(Queue colaEventos, TopicExchange exchangeEventos) {
        return BindingBuilder.bind(colaEventos)
                .to(exchangeEventos)
                .with(RK_EVENTO + ".#");
    }
}
