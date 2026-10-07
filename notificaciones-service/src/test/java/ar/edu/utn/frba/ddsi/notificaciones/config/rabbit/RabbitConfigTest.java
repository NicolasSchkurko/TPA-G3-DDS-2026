package ar.edu.utn.frba.ddsi.notificaciones.config.rabbit;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** La cola principal deriva a la dead letter, y la dead letter existe y está bien conectada. */
class RabbitConfigTest {

    private final RabbitConfig config = new RabbitConfig();

    @Test
    void laColaPrincipalDerivaLoQueNoSePuedeProcesarALaDeadLetter() {
        Queue cola = config.colaNotificaciones();

        assertEquals(RabbitConfig.EXCHANGE_DLQ,
                cola.getArguments().get("x-dead-letter-exchange"),
                "sin dead letter exchange, lo rechazado no tiene a dónde ir");
        assertEquals(RabbitConfig.COLA_DLQ,
                cola.getArguments().get("x-dead-letter-routing-key"));
        assertTrue(cola.isDurable());
    }

    @Test
    void laColaMuertaYSuExchangeExisten() {
        Queue colaDlq = config.colaDlq();

        assertNotNull(config.exchangeDlq(), "sin exchange, el dead letter no llega a la cola");
        assertEquals(RabbitConfig.COLA_DLQ, colaDlq.getName());
        assertTrue(colaDlq.isDurable(), "una cola no durable perdería lo irrecuperable al reiniciar");

        // El binding tiene que unir la cola muerta con la routing key que usa la cola principal.
        Binding bindingDlq = config.bindingDlq(colaDlq, config.exchangeDlq());

        assertEquals(RabbitConfig.COLA_DLQ, bindingDlq.getRoutingKey());
        assertEquals(RabbitConfig.COLA_DLQ, bindingDlq.getDestination());
    }
}
