package ar.edu.utn.frba.ddsi.logisticas.RabbitMQ;

import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.EntregaDTO;
import ar.edu.utn.frba.ddsi.logisticas.services.EntregaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * Recibe las donaciones que esperan ser entregadas y las registra como ítems de entrega.
 *
 * <p>Todas las instancias escuchan la misma cola salvo que esté configurado el reparto por shards:
 * el SpEL de {@code @RabbitListener} resuelve el bean {@code colasDeEstaInstancia}.
 *
 * <p>Reintenta con espera antes de rendirse, así que un fallo de infraestructura se vuelve blando.
 * Agotados los intentos, rendirse es lanzar {@link AmqpRejectAndDontRequeueException}: Spring AMQP
 * reencola por defecto, y reencolar lo devuelto a la cola compartida lo haría fallar otra vez y
 * frenarla para siempre. El rechazo sin requeue es lo que activa el dead letter de
 * {@code RabbitMQConfig}.
 */
@Component
public class DonacionListener {

    private static final Logger log = LoggerFactory.getLogger(DonacionListener.class);

    private final EntregaService entregaService;
    private final int intentos;
    private final long esperaMs;

    public DonacionListener(
            EntregaService entregaService,
            @Value("${logistica.reintentos:3}") int intentos,
            @Value("${logistica.espera-reintento-ms:2000}") long esperaMs) {
        this.entregaService = entregaService;
        this.intentos = intentos;
        this.esperaMs = esperaMs;
    }

    @RabbitListener(queues = "#{colasDeEstaInstancia}")
    public void recibirDonacionParaEntregar(EntregaDTO peticion) {
        RuntimeException ultimoFallo = null;

        for (int intento = 1; intento <= intentos; intento++) {
            try {
                entregaService.procesarPeticion(peticion);
                return;
            } catch (IllegalArgumentException errorDeNegocio) {
                // Un payload inválido falla igual las veces que lo intentes: no se gasta más
                // intentos ni se frena la cola.
                log.warn("Donación descartada por datos inválidos: {}", errorDeNegocio.getMessage());
                return;
            } catch (DataIntegrityViolationException yaRegistrada) {
                // La carrera benigna entre dos instancias: gana una y la otra recibe el error,
                // que es justo el estado final buscado.
                log.info("La donación ya fue registrada por otra instancia, mensaje descartado "
                        + "sin ir a la cola de mensajes muertos");
                return;
            } catch (RuntimeException error) {
                ultimoFallo = error;
                log.warn("Falló el procesamiento de una donación (intento {} de {}): {}",
                        intento, intentos, error.getMessage());
                if (intento < intentos) {
                    dormir();
                }
            }
        }

        // Rechazo sin requeue explícito: es lo que manda el mensaje a la DLQ.
        log.error("La donación no se pudo procesar en {} intentos, va a la cola de mensajes "
                + "muertos", intentos, ultimoFallo);
        throw new AmqpRejectAndDontRequeueException(
                "Donación no procesable tras " + intentos + " intentos", ultimoFallo);
    }

    /**
     * Espera entre intentos. Duerme el hilo del listener a propósito: la alternativa es tirar el
     * mensaje y que otro lo tome, y el mensaje ya se consumió de la cola, así que nadie más lo
     * va a leer.
     */
    private void dormir() {
        try {
            Thread.sleep(esperaMs);
        } catch (InterruptedException interrumpido) {
            Thread.currentThread().interrupt();
            log.debug("Se interrumpió la espera entre reintentos");
        }
    }
}