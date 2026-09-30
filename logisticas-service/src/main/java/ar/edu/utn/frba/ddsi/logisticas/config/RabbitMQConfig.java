package ar.edu.utn.frba.ddsi.logisticas.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String DONACIONES_EXCHANGE = "donaciones.exchange";
    public static final String NUEVAS_DONACIONES_QUEUE = "logisticas.donaciones.nuevas.queue";
    public static final String ROUTING_KEY_NUEVA_DONACION = "donaciones.creada";

    public static final String LOGISTICAS_EXCHANGE = "logisticas.exchange";
    public static final String EVENTOS_QUEUE = "donaciones.eventos.queue";
    public static final String ROUTING_KEY_EVENTOS = "logisticas.eventos";

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
    public Queue estadosEntregaQueue() {
        return new Queue(EVENTOS_QUEUE, true);
    }

    @Bean
    public Binding bindingEstadosEntrega(Queue estadosEntregaQueue, TopicExchange logisticasExchange) {
        return BindingBuilder.bind(estadosEntregaQueue).to(logisticasExchange).with(ROUTING_KEY_EVENTOS);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
