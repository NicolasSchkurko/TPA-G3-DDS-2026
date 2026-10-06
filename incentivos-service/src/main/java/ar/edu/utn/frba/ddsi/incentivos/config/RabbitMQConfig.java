package ar.edu.utn.frba.ddsi.incentivos.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topología del broker para las notificaciones que publica este servicio.
 *
 * <p><b>Declara el exchange y no la cola.</b> El exchange es el punto de encuentro entre quien
 * publica y quien consume: lo declara el que produce y los demás atan su cola. Si este módulo
 * declarara también la cola de notificaciones, el nombre quedaría acoplado a los dos lados y
 * cambiarlorequirería tocar los dos.
 *
 * <p><b>El converter se declara con el {@code ObjectMapper} de la aplicación</b> para respetar
 * los módulos de Jackson ya registrados. Sin este bean, Spring Boot deja el
 * {@code RabbitTemplate} con {@code SimpleMessageConverter}, que solo serializa
 * {@code byte[]}, {@code String} y {@code Serializable}. El DTO de notificación no es ninguno
 * de los tres, así que la publicación fallaba con {@code IllegalArgumentException} y el error
 * se perdía en el catch del productor: el servicio reportaba que había notificado y no se había
 * publicado nada.
 */
@Configuration
@EnableRabbit
public class RabbitMQConfig {

    /** Exchange donde publica este servicio; notificaciones ata su cola a esta routing key. */
    public static final String EXCHANGE_NOTIFICACIONES = "notificaciones.exchange";

    /** Routing key de los avisos de incentivo: misión completada, cambio de misión, categoría. */
    public static final String RK_INCENTIVO = "notificaciones.incentivo";

    /**
     * Exchange de integración con donaciones-service.
     *
     * <p>Este servicio no lo consume: lee la síntesis del donante por HTTP contra
     * {@code DonacionClient}. Se declara para que quede documentado que el exchange existe y que
     * este módulo no participa de esa dirección del flujo.
     */
    public static final String EXCHANGE_DONACIONES = "logistica.exchange";

    @Bean
    public TopicExchange exchangeNotificaciones() {
        return new TopicExchange(EXCHANGE_NOTIFICACIONES, true, false);
    }

    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}