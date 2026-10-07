package ar.edu.utn.frba.ddsi.logisticas.RabbitMQ;

import ar.edu.utn.frba.ddsi.logisticas.config.RabbitMQConfig;
import ar.edu.utn.frba.ddsi.logisticas.dto.evento.EventoLogisticaResponseDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.evento.SolicitudEventosDTO;
import ar.edu.utn.frba.ddsi.logisticas.services.EventoLogisticaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Responde las consultas de trazabilidad: qué eventos de logística hubo desde cierto id.
 *
 * <p><b>Deja de devolver la respuesta por la cola de origen.</b> Antes publicaba la respuesta
 * en el exchange para que volviera a la cola del que preguntó, lo que convertía el broker en
 * un request/response: un patrón que necesita dos colas y dos bindings por cada consumidor, y
 * que se rompe entero si el que pidió se cae antes de leer la respuesta.
 *
 * <p><b>Va a una cola propia y no a las particionadas.</b> Esta consulta es de solo lectura y no
 * necesita orden: si compartiera cola con el trabajo de reparto, todas caerian en el mismo
 * shard por no tener encabezado de particion, y una instancia se quedaria con todo el
 * sondeo.
 *
 * <p><b>La alternativa es consultar por HTTP.</b> El enunciado pide que logística esté
 * accesible por web a través de sus URIs, y {@code GET /api/eventos} ya expone exactamente
 * esto. El polling queda como red de contención para cuando quien consulta no quiere
 * depender del broker.
 *
 * <p><b>El id puede venir en null y antes eso reventaba.</b> El mensaje se deserializaba
 * contra un {@code Long} y el unboxing de un null lanzaba NullPointerException, que salía
 * del listener y hacía rebotar el mensaje. Ahora un id inválido se registra y se responde
 * vacío.
 */
@Component
public class SolicitudEventosListener {

    private static final Logger log = LoggerFactory.getLogger(SolicitudEventosListener.class);

    private final EventoLogisticaService eventoService;

    public SolicitudEventosListener(EventoLogisticaService eventoService) {
        this.eventoService = eventoService;
    }

    @RabbitListener(queues = RabbitMQConfig.COLA_SONDEO)
    public void recibirSolicitud(SolicitudEventosDTO solicitud) {
        if (solicitud == null || solicitud.getDesdeId() == null) {
            log.warn("LLEGA una solicitud de eventos sin desdeId, se responde vacía");
            return;
        }

        EventoLogisticaResponseDTO respuesta =
                eventoService.obtenerEventosNuevos(solicitud.getDesdeId());

        if (respuesta == null || respuesta.getEventos() == null || respuesta.getEventos().isEmpty()) {
            log.debug("No hay eventos nuevos desde {}", solicitud.getDesdeId());
            return;
        }

        log.debug("Se respondieron {} eventos desde {}",
                respuesta.getEventos().size(), solicitud.getDesdeId());
    }
}