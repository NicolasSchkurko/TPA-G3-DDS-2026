package ar.edu.utn.frba.ddsi.notificaciones.config.rabbit;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.amqp.RabbitRetryTemplateCustomizer;
import org.springframework.http.HttpStatus;
import org.springframework.retry.backoff.NoBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    @Test
    void losBindingsDeLaColaNoIncluyenLaRutaDirectaDeLogistica() throws Exception {
        RabbitConfig config = new RabbitConfig();
        Queue cola = config.colaNotificaciones();
        TopicExchange exchange = config.exchangeNotificaciones();
        Set<String> claves = new HashSet<>();

        for (Method metodo : RabbitConfig.class.getDeclaredMethods()) {
            if (metodo.getReturnType() == Binding.class
                    && metodo.getParameterCount() == 2
                    && metodo.getName().startsWith("bindingNotificaciones")) {
                claves.add(((Binding) metodo.invoke(config, cola, exchange)).getRoutingKey());
            }
        }

        // La ruta real es logística -> donaciones -> notificaciones; un binding directo
        // notificaciones.evento.logistica haría creer que logística habla con este servicio.
        assertEquals(Set.of(RabbitConfig.RK_AVISO, RabbitConfig.RK_INCENTIVO,
                RabbitConfig.RK_DONACION), claves);
    }

    @Test
    void elAvisoInternoTieneRoutingKeyYBindingPropio() {
        assertEquals("notificaciones.aviso", RabbitConfig.RK_AVISO);
        Binding bindingAviso = config.bindingNotificacionesAviso(
                config.colaNotificaciones(), config.exchangeNotificaciones());
        assertEquals(RabbitConfig.RK_AVISO, bindingAviso.getRoutingKey());
        assertEquals(RabbitConfig.COLA_NOTIFICACIONES, bindingAviso.getDestination());
    }

    @Test
    void losErroresPermanentesNoSeReintentanYLosTransitoriosSi() {
        assertEquals(1, intentosHastaFallar(new AmqpRejectAndDontRequeueException("4xx de n8n")),
                "un error permanente no debe reintentarse: va directo a la DLQ");
        assertEquals(3, intentosHastaFallar(new ResourceAccessException("timeout contra n8n")));
        assertEquals(3, intentosHastaFallar(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR)));
    }

    private int intentosHastaFallar(Throwable error) {
        RetryTemplate template = new RetryTemplate();
        template.setBackOffPolicy(new NoBackOffPolicy());
        config.retryNoReintentaErroresPermanentes(3)
                .customize(RabbitRetryTemplateCustomizer.Target.LISTENER, template);

        int[] intentos = {0};
        assertThrows(Exception.class, () -> template.execute(contexto -> {
            intentos[0]++;
            throw error;
        }));
        return intentos[0];
    }
}
