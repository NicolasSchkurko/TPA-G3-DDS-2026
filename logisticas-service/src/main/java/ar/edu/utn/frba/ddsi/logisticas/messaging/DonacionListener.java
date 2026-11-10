package ar.edu.utn.frba.ddsi.logisticas.messaging;

import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.EntregaDTO;
import ar.edu.utn.frba.ddsi.logisticas.services.EntregaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

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
            } catch (DataIntegrityViolationException carrera) {
                // Carrera de duplicados o clave primaria: otra instancia ya la registró con éxito
                log.info("Donación ya registrada por otra instancia concurrente: {}", carrera.getMessage());
                return;
            } catch (RuntimeException error) {
                ultimoFallo = error;
                log.warn("Falló el procesamiento de una donación (intento {} de {}): {}",
                        intento, intentos, error.getMessage());
                if (intento < intentos && !dormir()) {
                    // Si el contexto se está cerrando, el hilo se interrumpe durante la espera:
                    // no tiene sentido seguir reintentando contra un servicio que se apaga.
                    throw new AmqpRejectAndDontRequeueException(
                            "Listener interrumpido durante la espera entre reintentos", ultimoFallo);
                }
            }
        }

        // Rechazo sin requeue explícito: es lo que manda el mensaje a la DLQ.
        log.error("La donación no se pudo procesar en {} intentos, va a la cola de mensajes "
                + "muertos", intentos, ultimoFallo);
        throw new AmqpRejectAndDontRequeueException(
                "Donación no procesable tras " + intentos + " intentos", ultimoFallo);
    }

    private boolean dormir() {
        try {
            Thread.sleep(esperaMs);
            return true;
        } catch (InterruptedException interrumpido) {
            Thread.currentThread().interrupt();
            log.debug("Se interrumpió la espera entre reintentos");
            return false;
        }
    }
}