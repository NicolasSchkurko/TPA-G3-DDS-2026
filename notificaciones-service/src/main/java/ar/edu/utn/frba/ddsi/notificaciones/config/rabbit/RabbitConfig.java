package ar.edu.utn.frba.ddsi.notificaciones.config.rabbit;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String COLA_NOTIFICACIONES = "notificaciones";

    @Bean
    public Queue queue() {
        return new Queue(COLA_NOTIFICACIONES, true);
    }
}