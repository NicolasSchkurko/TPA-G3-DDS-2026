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
 * Topología del broker para las notificaciones: declara el exchange (no la cola, que ata el
 * consumidor) y un converter JSON con el {@code ObjectMapper} de la aplicación.
 */
@Configuration
@EnableRabbit
public class RabbitMQConfig {

    /** Exchange donde publica este servicio; notificaciones ata su cola a esta routing key. */
    public static final String EXCHANGE_NOTIFICACIONES = "notificaciones.exchange";

    /** Routing key de los avisos de incentivo: misión completada, cambio de misión, categoría. */
    public static final String RK_INCENTIVO = "notificaciones.incentivo";

    @Bean
    public TopicExchange exchangeNotificaciones() {
        return new TopicExchange(EXCHANGE_NOTIFICACIONES, true, false);
    }

    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}