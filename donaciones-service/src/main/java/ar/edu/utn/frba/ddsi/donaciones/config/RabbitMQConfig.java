package ar.edu.utn.frba.ddsi.donaciones.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topología del broker desde el lado de {@code donaciones-service}.
 *
 * <p><b>Antes declaraba las colas y los bindings de logística, y eso estaba mal.</b> El
 * servicio publicaba en el exchange de logística pero no le tocaba definir cómo logística
 * consume: la declaración de la cola ajena quedaba en este módulo. El síntoma era que
 * logistics-service tenía su propia copia de estos beans, idéntica salvo por dos bindings
 * atados al exchange equivocado. Con las dos declaraciones, el broker aceptaba ambas y el
 * binding bueno de este módulo tapaba el malo del otro. Levantando logística sola, las
 * colas de este módulo no existían y la cadena se cortaba.
 *
 * <p><b>La frontera queda así:</b> este servicio publica en el exchange de integración y
 * escucha la cola de eventos que logística le deja para trazabilidad. Las colas que logística
 * consume las declara logística.
 *
 * <p><b>El converter se declara con el ObjectMapper de la aplicación</b> para respetar los
 * módulos de Jackson ya registrados. Sin este bean, Boot deja el {@code RabbitTemplate} con
 * {@code SimpleMessageConverter}, que solo serializa {@code byte[]}, {@code String} y
 * {@code Serializable}: los DTO de integración no son ninguno y la publicación fallaba con
 * la excepción tragada en el catch del productor.
 */
@Configuration
@EnableRabbit
public class RabbitMQConfig {

    /** Exchange de integración con logística. Lo declara este módulo y logística lo usa. */
    public static final String EXCHANGE_INTEGRACION = "logistica.exchange";

    /** Routing key de las donaciones nuevas que esperan planificarse. */
    public static final String RK_NUEVA_DONACION = "donaciones.creada";

    /**
     * Routing key con el que este servicio le pide a logística los eventos de trazabilidad
     * que todavía no vio.
     *
     * <p>Va por el exchange de integración porque es logística quien responde, pero el
     * enunciado cubre la trazabilidad por HTTP con {@code GET /api/eventos}: el polling es la
     * red de contención para recuperar eventos perdidos mientras el broker estuvo caído.
     */
    public static final String RK_SOLICITUD_EVENTOS = "logistica.solicitud.eventos";

    /** Exchange de eventos de trazabilidad que logística publica. */
    public static final String EXCHANGE_EVENTOS = "logistica.eventos.exchange";

    /** Routing key de los eventos de trazabilidad. */
    public static final String RK_EVENTO = "logistica.evento";

    /** Cola propia donde este servicio consume los eventos de trazabilidad. */
    public static final String COLA_EVENTOS = "donaciones.eventos.queue";

    /**
     * Exchange de notificaciones.
     *
     * <p>Lo declara este servicio porque es el que publica, y notificaciones ata su cola. El
     * enunciado pide que la integracion con el servicio de notificaciones sea asincrona por
     * cola de mensajes, y lo dice para los dos servicios de dominio: este y el de incentivos.
     * Los dos publican al mismo exchange con routing keys distintas.
     */
    public static final String EXCHANGE_NOTIFICACIONES = "notificaciones.exchange";

    /** Routing key de las notificaciones de MetaDonacion: alta de donante, donacion asignada, entregas. */
    public static final String RK_DONACION = "notificaciones.donacion";

    @Bean
    public TopicExchange exchangeNotificaciones() {
        return new TopicExchange(EXCHANGE_NOTIFICACIONES, true, false);
    }

    /**
     * Declara solo el exchange, no la cola de entrada de logística.
     *
     * <p>Un exchange es un punto de encuentro: lo declara quien publica y lo usan todos los
     * que consumen. Por eso este módulo lo define y logística se limita a atar su cola.
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
    public Queue colaEventos() {
        return new Queue(COLA_EVENTOS, true);
    }

    @Bean
    public Binding bindingEventos(Queue colaEventos, TopicExchange exchangeEventos) {
        return BindingBuilder.bind(colaEventos)
                .to(exchangeEventos)
                .with(RK_EVENTO + ".#");
    }

    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}