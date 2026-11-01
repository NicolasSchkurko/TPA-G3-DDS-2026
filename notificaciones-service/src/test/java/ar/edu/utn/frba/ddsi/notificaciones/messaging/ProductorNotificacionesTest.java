package ar.edu.utn.frba.ddsi.notificaciones.messaging;

import ar.edu.utn.frba.ddsi.notificaciones.config.rabbit.RabbitConfig;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ProductorNotificacionesTest {

    static class StubRabbitTemplate extends RabbitTemplate {
        String ultimoExchange;
        String ultimoRoutingKey;
        Object ultimoMensaje;

        @Override
        public void convertAndSend(String exchange, String routingKey, Object object) {
            this.ultimoExchange = exchange;
            this.ultimoRoutingKey = routingKey;
            this.ultimoMensaje = object;
        }
    }

    @Test
    @DisplayName("Punto 31: El aviso interno se publica con la routing key propia 'notificaciones.aviso' y no con 'notificaciones.incentivo'")
    void publicarAvisoUsaRoutingKeyPropiaAviso() {
        StubRabbitTemplate stubTemplate = new StubRabbitTemplate();
        ProductorNotificaciones productor = new ProductorNotificaciones(stubTemplate);

        Notificacion notificacion = new Notificacion("ana@test.com", "email", new Mensaje("A", "C"));
        productor.enviar(notificacion);

        assertEquals(RabbitConfig.EXCHANGE_NOTIFICACIONES, stubTemplate.ultimoExchange);
        assertEquals(RabbitConfig.RK_AVISO, stubTemplate.ultimoRoutingKey);
        assertEquals("notificaciones.aviso", stubTemplate.ultimoRoutingKey);
        assertNotEquals(RabbitConfig.RK_INCENTIVO, stubTemplate.ultimoRoutingKey);

        assertInstanceOf(ConsumidorNotificaciones.AvisoNotificacion.class, stubTemplate.ultimoMensaje);
        ConsumidorNotificaciones.AvisoNotificacion aviso =
                (ConsumidorNotificaciones.AvisoNotificacion) stubTemplate.ultimoMensaje;
        assertEquals(notificacion.getId().toString(), aviso.id());
        assertEquals("email", aviso.medioDeContacto());
        assertEquals("ana@test.com", aviso.direccionDeContacto());
    }
}
