package ar.edu.utn.frba.ddsi.donaciones.models.sheduler;

import ar.edu.utn.frba.ddsi.donaciones.config.RabbitMQConfig;
import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.SolicitudEventosDTO;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Consulta periódicamente a logística si hubo eventos nuevos de trazabilidad.
 *
 * <p><b>Es red de contención, no el mecanismo principal.</b> Logística publica los eventos en
 * un exchange y este servicio los recibe por {@code EventosListener}, así que el polling solo
 * sirve para dos casos: recuperar un evento que se perdió mientras el broker estuvo caído, o
 * arrancar el listener después de que ya hubiera eventos. Por eso el intervalo es largo y no
 * compite con el listener por el trabajo.
 *
 * <p><b>Publica al exchange de eventos, no al de integración.</b> Antes mandaba
 * {@code ROUTING_KEY_SOLICITUD_EVENTOS} al exchange de integración, que es el que lleva las
 * donaciones a planificar: la consulta de trazabilidad viajaba por el canal equivocado y
 * terminaba en la cola de entrada de logística, mezclada con las donaciones.
 *
 * <p><b>El cursor es propio y arranca en 0.</b> Los ids de evento son 1-based, así que
 * arrancar en 0 pide "todo lo que haya". Antes tomaba el cursor del listener y lo
 * modificaba desde acá, con lo que los dos beans peleaban por el mismo estado.
 */
@Getter
@Component
public class LogisticaPollingScheduler {

    private static final Logger log = LoggerFactory.getLogger(LogisticaPollingScheduler.class);

    private final RabbitTemplate rabbitTemplate;

    /** Último id solicitado. Empieza en 0 para pedir todo el historial la primera vez. */
    private long ultimoIdProcesado = 0L;

    public LogisticaPollingScheduler(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /** Cada 5 minutos. El listener cubre el camino normal; esto solo recupera faltantes. */
    @Scheduled(fixedDelay = 300000)
    public void buscarNuevosEventosLogistica() {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_INTEGRACION,
                    RabbitMQConfig.RK_SOLICITUD_EVENTOS,
                    new SolicitudEventosDTO(ultimoIdProcesado)
            );

            log.debug("Se publicó la solicitud de eventos desde {}", ultimoIdProcesado);
        } catch (RuntimeException error) {
            // No relanza: un polling que revienta mataría el hilo del scheduler y el resto de
            // las tareas programadas del proceso.
            log.warn("No se pudo solicitar los eventos de logística: {}", error.getMessage());
        }
    }
}