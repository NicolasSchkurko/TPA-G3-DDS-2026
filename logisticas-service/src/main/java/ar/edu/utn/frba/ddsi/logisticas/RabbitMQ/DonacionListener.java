package ar.edu.utn.frba.ddsi.logisticas.RabbitMQ;

import ar.edu.utn.frba.ddsi.logisticas.config.RabbitMQConfig;
import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.EntregaDTO;
import ar.edu.utn.frba.ddsi.logisticas.services.EntregaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Recibe las donaciones que esperan ser entregadas y las registra como ítems de entrega.
 *
 * <p><b>Es uno de los N consumidores de la cola compartida.</b> La cola
 * {@code COLA_INTEGRACION} la comparten todas las instancias de logística y el broker
 * reparte los mensajes de a uno, así que varias instancias procesan en paralelo repartiendo
 * trabajo. Levantar una segunda instancia, para cumplir el "más de un servicio de logística
 * disponible" del enunciado, es arrancar el mismo jar otra vez apuntando al mismo broker.
 *
 * <p><b>El fallo se relanza a propósito.</b> Con la cola configurada con dead letter exchange,
 * relanzar manda el mensaje a la cola de mensajes muertos en vez de dejarlo perdido: antes el
 * catch se lo tragaba y la donación se perdía sin dejar rastro.
 *
 * <p><b>Los errores de negocio no se relanzan.</b> Un payload inválido va a fallar igual en
 * cada reintento, y relanzarlo bloquearía la cola compartida, frenando las donaciones de las
 * demás instancias.
 */
@Component
public class DonacionListener {

    private static final Logger log = LoggerFactory.getLogger(DonacionListener.class);

    private final EntregaService entregaService;

    public DonacionListener(EntregaService entregaService) {
        this.entregaService = entregaService;
    }

    @RabbitListener(queues = RabbitMQConfig.COLA_INTEGRACION)
    public void recibirDonacionParaEntregar(EntregaDTO peticion) {
        try {
            entregaService.procesarPeticion(peticion);
        } catch (IllegalArgumentException errorDeNegocio) {
            log.warn("Donación descartada por datos inválidos: {}", errorDeNegocio.getMessage());
        } catch (RuntimeException error) {
            log.error("Falló el procesamiento de una donación, va a la cola de mensajes muertos",
                    error);
            throw error;
        }
    }
}