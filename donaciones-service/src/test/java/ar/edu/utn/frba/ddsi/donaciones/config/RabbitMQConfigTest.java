package ar.edu.utn.frba.ddsi.donaciones.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.CustomExchange;

import static org.junit.jupiter.api.Assertions.*;

public class RabbitMQConfigTest {

    @Test
    @DisplayName("Punto 50: RabbitMQConfig declara el exchange consistent-hash para integracion con logistica")
    void rabbitMQConfig_declaraExchangeIntegracionHash() {
        RabbitMQConfig config = new RabbitMQConfig();
        CustomExchange exchange = config.exchangeIntegracionHash();

        assertNotNull(exchange);
        assertEquals(RabbitMQConfig.EXCHANGE_INTEGRACION_HASH, exchange.getName());
        assertEquals("x-consistent-hash", exchange.getType());
        assertTrue(exchange.isDurable());
        assertFalse(exchange.isAutoDelete());
        assertEquals("x-id-donacion", exchange.getArguments().get("hash-header"));
    }
}
