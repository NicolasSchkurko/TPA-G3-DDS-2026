package ar.edu.utn.frba.ddsi.notificaciones.config.rabbit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

/**
 * Converter JSON sin {@code __TypeId__}: publica con Jackson y al recibir entrega el body crudo, para
 * no acoplar el módulo a las clases del productor.
 */
@Configuration
public class MessageConverterConfig {

    private static final String CONTENT_TYPE_JSON = "application/json";

    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        return new MessageConverter() {

            @Override
            public Message toMessage(Object objeto, MessageProperties propiedades) {
                byte[] cuerpo;

                try {
                    cuerpo = objectMapper.writeValueAsBytes(objeto);
                } catch (Exception error) {
                    throw new MessageConversionException(
                            "No se pudo serializar el mensaje a JSON", error);
                }

                propiedades.setContentType(CONTENT_TYPE_JSON);
                propiedades.setContentEncoding("UTF-8");
                propiedades.setHeader("__TypeId__", null);

                return new Message(cuerpo, propiedades);
            }

            @Override
            public Object fromMessage(Message mensaje) {
                return new String(mensaje.getBody(), StandardCharsets.UTF_8);
            }
        };
    }
}
