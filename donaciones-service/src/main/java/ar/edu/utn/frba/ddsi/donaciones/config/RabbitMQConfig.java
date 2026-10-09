package ar.edu.utn.frba.ddsi.donaciones.config;

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
 * Topología del broker desde el lado de donaciones-service: publica en el exchange de
 * integración, consume la cola de eventos de trazabilidad y las notificaciones. Cada
 * servicio declara lo suyo: las colas que consume logística las declara logística.
 */
@Configuration
@EnableRabbit
public class RabbitMQConfig {

    /**
     * Exchange consistent-hash que reparte las donaciones entre las instancias de logística.
     * Es el que hay que usar para publicar una donación: publicar a otro exchange deja el
     * mensaje en una cola sin consumidor y se pierde en silencio.
     */
    public static final String EXCHANGE_INTEGRACION_HASH = "logistica.integracion.hash";

    /** Exchange de eventos de trazabilidad que logisticas-service publica. */
    public static final String EXCHANGE_EVENTOS = "logistica.eventos.exchange";

    /** Exchange de notificaciones. */
    public static final String EXCHANGE_NOTIFICACIONES = "notificaciones.exchange";

    /** Routing key de las donaciones nuevas que esperan planificarse. */
    public static final String RK_NUEVA_DONACION = "donaciones.creada";

    /** Routing key de los eventos de trazabilidad que logisticas-service publica. */
    public static final String RK_EVENTO = "logistica.evento";

    /**
     * Routing key de las notificaciones de donaciones-service: alta de donante, donacion
     * asignada, entregas.
     */
    public static final String RK_DONACION = "notificaciones.donacion";

    /** Clave del exchange consistent-hash de logística: el broker enruta por el hash de este
     *  encabezado, así todos los mensajes de la misma donación caen en la misma cola y los
     *  procesa la misma instancia en orden. El nombre lo declara {@code CustomExchange} de
     *  Spring AMQP: es contrato entre los dos servicios. */
    public static final String HEADER_PARTICION = "x-id-donacion";

    /** Cola propia donde este servicio consume los eventos de trazabilidad. */
    public static final String COLA_EVENTOS = "donaciones.eventos.queue";

    /**
     * El exchange de eventos de trazabilidad: lo declara logística como quien publica, y este
     * servicio lo usa para consumir. Un exchange es un punto de encuentro: ninguno de los dos
     * tiene que saber el nombre de la cola del otro.
     */
    @Bean
    public TopicExchange exchangeEventos() {
        return new TopicExchange(EXCHANGE_EVENTOS, true, false);
    }

    @Bean
    public TopicExchange exchangeNotificaciones() {
        return new TopicExchange(EXCHANGE_NOTIFICACIONES, true, false);
    }

    /** La cola donde este servicio consume los eventos de trazabilidad. El binding de abajo
     *  la recibe por parámetro: sin este bean el contexto no arranca, y compila igual. */
    @Bean
    public Queue colaEventos() {
        return QueueBuilder.durable(COLA_EVENTOS).build();
    }

    /** Clave exacta de la publicación de logística: publica todos los tipos con la misma RK
     *  ({@code "logistica.evento"}) y el tipo viaja en el cuerpo. Con {@code ".#"} el binding
     *  no matchearía: en un topic exchange el comodín exige al menos un nivel más (el mismo
     *  error que ya mordió con notificaciones). */
    @Bean
    public Binding bindingEventos(Queue colaEventos, TopicExchange exchangeEventos) {
        return BindingBuilder.bind(colaEventos)
                .to(exchangeEventos)
                .with(RK_EVENTO);
    }

    /** Serializa a JSON. Sin este bean el listener recibe el body crudo y falla con
     *  {@code MessageConversionException: Cannot convert from [[B] to [EventoLogisticaDTO]}:
     *  el {@code RabbitTemplate} default solo sabe manejar {@code byte[]/String/Serializable}.
     *  Compila igual sin él; el error aparece solo cuando llega un evento de verdad. */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
