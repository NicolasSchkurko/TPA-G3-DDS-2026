package ar.edu.utn.frba.ddsi.notificaciones.config.rabbit;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topología del broker: el exchange, la cola y su dead letter. Si la cola ya existía sin los
 * argumentos de dead letter, hay que borrarla una vez (RabbitMQ no redeclara distinto).
 */
@Configuration
@EnableRabbit
public class RabbitConfig {

    public static final String EXCHANGE_NOTIFICACIONES = "notificaciones.exchange";
    public static final String RK_INCENTIVO = "notificaciones.incentivo";
    public static final String RK_EVENTO_LOGISTICA = "notificaciones.evento.logistica";
    public static final String RK_DONACION = "notificaciones.donacion";
    public static final String COLA_NOTIFICACIONES = "notificaciones";

    public static final String EXCHANGE_DLQ = "notificaciones.dlq.exchange";
    public static final String COLA_DLQ = "notificaciones.dlq";

    @Bean
    public TopicExchange exchangeNotificaciones() {
        return new TopicExchange(EXCHANGE_NOTIFICACIONES, true, false);
    }

    @Bean
    public Queue colaNotificaciones() {
        return QueueBuilder.durable(COLA_NOTIFICACIONES)
                .deadLetterExchange(EXCHANGE_DLQ)
                .deadLetterRoutingKey(COLA_DLQ)
                .build();
    }

    @Bean
    public TopicExchange exchangeDlq() {
        return new TopicExchange(EXCHANGE_DLQ, true, false);
    }

    @Bean
    public Queue colaDlq() {
        return new Queue(COLA_DLQ, true);
    }

    @Bean
    public Binding bindingDlq(Queue colaDlq, TopicExchange exchangeDlq) {
        return BindingBuilder.bind(colaDlq)
                .to(exchangeDlq)
                .with(COLA_DLQ);
    }

    @Bean
    public Binding bindingNotificaciones(Queue colaNotificaciones, TopicExchange exchangeNotificaciones) {
        return BindingBuilder.bind(colaNotificaciones)
                .to(exchangeNotificaciones)
                .with(RK_INCENTIVO);
    }

    @Bean
    public Binding bindingNotificacionesEventosLogistica(
            Queue colaNotificaciones, TopicExchange exchangeNotificaciones) {
        return BindingBuilder.bind(colaNotificaciones)
                .to(exchangeNotificaciones)
                .with(RK_EVENTO_LOGISTICA);
    }

    @Bean
    public Binding bindingNotificacionesDonacion(
            Queue colaNotificaciones, TopicExchange exchangeNotificaciones) {
        return BindingBuilder.bind(colaNotificaciones)
                .to(exchangeNotificaciones)
                .with(RK_DONACION);
    }
}
