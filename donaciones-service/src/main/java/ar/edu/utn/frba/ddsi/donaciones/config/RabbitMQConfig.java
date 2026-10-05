package ar.edu.utn.frba.ddsi.donaciones.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String DONACIONES_EXCHANGE = "donaciones.exchange";
    public static final String NUEVAS_DONACIONES_QUEUE = "logisticas.donaciones.nuevas.queue";
    public static final String ROUTING_KEY_NUEVA_DONACION = "donaciones.creada";

    public static final String LOGISTICAS_EXCHANGE = "logisticas.exchange";
    public static final String RESPUESTA_EVENTOS_QUEUE = "logisticas.eventos.respuesta.queue";
    public static final String ROUTING_KEY_SOLICITUD_EVENTOS = "logisticas.solicitud.eventos";
    public static final String ROUTING_KEY_RESPUESTA_EVENTOS = "logisticas.eventos.respuesta";

    @Bean
    public TopicExchange donacionesExchange() {
        return new TopicExchange(DONACIONES_EXCHANGE);
    }

    @Bean
    public Queue nuevasDonacionesQueue() {
        return new Queue(NUEVAS_DONACIONES_QUEUE, true);
    }

    @Bean
    public Binding bindingNuevasDonaciones(Queue nuevasDonacionesQueue, TopicExchange donacionesExchange) {
        return BindingBuilder.bind(nuevasDonacionesQueue).to(donacionesExchange).with(ROUTING_KEY_NUEVA_DONACION);
    }

    @Bean
    public TopicExchange logisticasExchange() {
        return new TopicExchange(LOGISTICAS_EXCHANGE);
    }

    @Bean
    public Queue respuestaEventosQueue() {
        return new Queue(RESPUESTA_EVENTOS_QUEUE, true);
    }

    @Bean
    public Binding bindingRespuestaEventos(Queue respuestaEventosQueue,
                                           @Qualifier("logisticasExchange") TopicExchange logisticasExchange) {
        return BindingBuilder.bind(respuestaEventosQueue).to(logisticasExchange).with(ROUTING_KEY_RESPUESTA_EVENTOS);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
