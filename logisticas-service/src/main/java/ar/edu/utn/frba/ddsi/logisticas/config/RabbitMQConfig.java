package ar.edu.utn.frba.ddsi.logisticas.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topología del broker de integración con logística.
 *
 * <p><b>Solo declara el exchange y la cola de entrada; no el binding de la cola de
 * respuesta.</b> Ese binding estaba duplicado en el {@code RabbitMQConfig} de
 * @code donaciones-service{code}, y ataba la cola de solicitudes
 * de eventos al exchange equivocado (donaciones.exchange). Funcionaba solo porque
 * donaciones-service declaraba el mismo binding contra el exchange correcto y el broker
 * acumulaba las dos declaraciones. Con la cola atada al exchange equivocado,
 * levantar logistica sola dejaba las solicitudes de eventos sin ruta.
 *
 * <p><b>Competing consumers, que es lo que pide el enunciado.</b> El enunciado dice que el
 * broker debe permitir seleccionar entre más de un servicio de logística. Se cumple con un
 * exchange y una sola cola compartida por todas las instancias: el broker reparte los
 * mensajes entre ellas de a uno, así que N instancias de logística procesan en paralelo
 * repartiendo trabajo. Levantar una segunda instancia (el "servicio potencial" del
 * enunciado) es arrancar el mismo jar otra vez apuntando al mismo broker: no hay
 * configuración nueva.
 *
 * <p>Para poder reiniciar una instancia sin que se lleve mensajes que no procesó, la cola
 * tiene dead letter exchange: lo que no se puede procesar queda en la cola de
 * mensajes muertos en vez de rebotar entre consumidores.
 */
@Configuration
@EnableRabbit
public class RabbitMQConfig {

    /** Exchange de integración con logística. */
    public static final String EXCHANGE_INTEGRACION = "logistica.exchange";

    /**
     * Exchange de los eventos de trazabilidad que este servicio publica.
     *
     * <p>Es el punto de salida del patrón public/subscribe del enunciado: logística publica
     * el hecho (inicio de ruta, entrega realizada, entrega no satisfactoria) y
     * {@code donaciones-service} lo consume para notificar. Existe separado del exchange de integración
     * porque los flujos son distintos en dirección: por este entra trabajo, por el otro salen
     * eventos. Mezclarlos haría que un listener de integración recibiera eventos de trazabilidad
     * y viceversa.
     */
    public static final String EXCHANGE_EVENTOS = "logistica.eventos.exchange";

    /** Routing key de los eventos de trazabilidad. */
    public static final String RK_EVENTO = "logistica.evento";

    /** Routing key de las donaciones nuevas que esperan planificarse. */
    public static final String RK_NUEVA_DONACION = "donaciones.creada";

    /** Routing key de la solicitud de eventos de trazabilidad. */
    public static final String RK_SOLICITUD_EVENTOS = "logistica.solicitud.eventos";

    /** Cola compartida: varias instancias de logística compiten por leerla. */
    public static final String COLA_INTEGRACION = "logistica.integracion.queue";

    /** Dead letter de la cola de integración. */
    public static final String DLQ_INTEGRACION = "logistica.integracion.dlq";

    @Bean
    public TopicExchange exchangeIntegracion() {
        return new TopicExchange(EXCHANGE_INTEGRACION, true, false);
    }

    @Bean
    public TopicExchange exchangeEventos() {
        return new TopicExchange(EXCHANGE_EVENTOS, true, false);
    }

    @Bean
    public Queue colaIntegracion() {
        return QueueBuilder.durable(COLA_INTEGRACION)
                .deadLetterExchange("")
                .deadLetterRoutingKey(DLQ_INTEGRACION)
                .build();
    }

    @Bean
    public Queue deadLetterIntegracion() {
        return QueueBuilder.durable(DLQ_INTEGRACION).build();
    }

    @Bean
    public Binding bindingIntegracion(Queue colaIntegracion, TopicExchange exchangeIntegracion) {
        return BindingBuilder.bind(colaIntegracion)
                .to(exchangeIntegracion)
                .with(RK_NUEVA_DONACION);
    }

    @Bean
    public Binding bindingSolicitudEventos(Queue colaIntegracion, TopicExchange exchangeIntegracion) {
        return BindingBuilder.bind(colaIntegracion)
                .to(exchangeIntegracion)
                .with(RK_SOLICITUD_EVENTOS);
    }

    /**
     * Serializa a JSON.
     *
     * <p>Sin esto, Boot deja el {@code RabbitTemplate} con {@code SimpleMessageConverter},
     * que solo maneja {@code byte[]}, {@code String} y {@code Serializable}. Los DTO de
     * integración no son ninguno, así que la publicación fallaba y el error se perdía en el
     * {@code catch} del productor.
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
