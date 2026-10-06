package ar.edu.utn.frba.ddsi.notificaciones.config.rabbit;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topología del broker de notificaciones.
 *
 * <p><b>Es un exchange y no una cola suelta porque hay varios servicios hablando.</b> El
 * enunciado pide que la integración de los servicios de dominio con el de notificaciones sea
 * asíncrona a través de una cola de mensajes, y tanto el de donaciones como el de incentivos
 * publican. Con un exchange topic cada consumidor declara su propia cola, y agregar un tercer
 * productor no obliga a tocar a los que ya están.
 *
 * <p><b>Por qué el converter es un bean y no el de Boot.</b> Sin declarar
 * {@link MessageConverter}, Spring Boot deja el {@code RabbitTemplate} con
 * {@code SimpleMessageConverter}, que solo sabe manejar {@code byte[]}, {@code String} y
 * {@code Serializable}. Los DTO de integración no son ninguno de los tres, así que la
 * publicación fallaba con {@code IllegalArgumentException} dentro del {@code catch} del
 * productor: el servicio respondía 202 y el mensaje nunca salía.
 */
@Configuration
@EnableRabbit
public class RabbitConfig {

    /** Nombre del exchange donde publican los servicios de dominio. */
    public static final String EXCHANGE_NOTIFICACIONES = "notificaciones.exchange";

    /** Routing key de los avisos de incentivo: misión completada, cambio de misión, insignia. */
    public static final String RK_INCENTIVO = "notificaciones.incentivo";

    /** Routing key de los eventos de logística. */
    public static final String RK_EVENTO_LOGISTICA = "notificaciones.evento.logistica";

    /**
     * Routing key de las notificaciones de {@code MetaDonacion}: alta de donante, donación
     * asignada, resultado de una entrega.
     *
     * <p>Es una tercera routing key y no una extensión de las otras porque la cola se
     * declara con la clave exacta, no con un comodín: es la forma de no arrastrar de acá los
     * nombres de routing key de los otros servicios.
     */
    public static final String RK_DONACION = "notificaciones.donacion";

    /** Cola del propio servicio de notificaciones: consume lo que él mismo publica. */
    public static final String COLA_NOTIFICACIONES = "notificaciones";

    @Bean
    public TopicExchange exchangeNotificaciones() {
        return new TopicExchange(EXCHANGE_NOTIFICACIONES, true, false);
    }

    @Bean
    public Queue colaNotificaciones() {
        return new Queue(COLA_NOTIFICACIONES, true);
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
