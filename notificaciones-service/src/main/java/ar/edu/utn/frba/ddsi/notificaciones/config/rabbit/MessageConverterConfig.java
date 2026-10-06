package ar.edu.utn.frba.ddsi.notificaciones.config.rabbit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Converter de mensajes que escribe JSON y devuelve el payload crudo.
 *
 * <p><b>Existe por una razón medida, no por gusto.</b> {@code Jackson2JsonMessageConverter}
 * escribe por defecto un encabezado {@code __TypeId__} con el nombre de la clase Java del
 * productor, y del otro lado intenta resolver esa clase. Cada aviso de
 * {@code MetaDonacion} moría en el consumidor con
 * {@code failed to resolve class name. Class not found [PerfilNotificacionDTO]}, porque este
 * módulo no tiene —ni debe tener— la clase del productor.
 *
 * <p>Eso además rompía una regla del enunciado: los servicios de dominio no deben compartir
 * modelo, y el {@code __TypeId__} es un acoplamiento a nivel de bytecode entre servicios que
 * no se conocen. El enunciado define el contrato como el JSON: los nombres de los campos.
 *
 * <p><b>Por qué no se usa Jackson2JsonMessageConverter con el tipo deshabilitado.</b> Es la
 * solución obvia, pero con el type mapper en {@code null} el converter desreferencia el tipo
 * destino y tira {@code NullPointerException} antes de llegar al listener, que es
 * exactamente lo que se midió. Desactivar el tipo no alcanza: hay que evitar el converter.
 *
 * <p>Por eso la conversión va a mano en las dos direcciones:
 *
 * <ul>
 *   <li><b>Al publicar:</b> Jackson serializa el objeto. El {@code ObjectMapper} es el de la
 *       aplicación, así respeta los módulos ya registrados (fechas ISO, parámetros nulos).
 *   <li><b>Al recibir:</b> se devuelve el body como {@code String}, sin deserializar a nada.
 *       El listener decide el tipo por el contenido del JSON.
 * </ul>
 *
 * <p>El contenido de la propiedad queda como texto plano, que es lo que espera el listener
 * crudo.
 */
@Configuration
public class MessageConverterConfig {

    /** Content type con el que viaja el payload. */
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
                    throw new org.springframework.amqp.support.converter
                            .MessageConversionException(
                            "No se pudo serializar el mensaje a JSON", error);
                }

                propiedades.setContentType(CONTENT_TYPE_JSON);
                propiedades.setContentEncoding("UTF-8");

                // Sin encabezado de tipo: el receptor no tiene la clase del productor y
                // tampoco debe necesitarla.
                propiedades.setHeader("__TypeId__", null);

                return new Message(cuerpo, propiedades);
            }

            @Override
            public Object fromMessage(Message mensaje) {
                return new String(mensaje.getBody(), java.nio.charset.StandardCharsets.UTF_8);
            }
        };
    }
}