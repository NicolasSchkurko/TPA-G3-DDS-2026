package ar.edu.utn.frba.ddsi.notificaciones.messaging;

import ar.edu.utn.frba.ddsi.notificaciones.config.rabbit.RabbitConfig;
import ar.edu.utn.frba.ddsi.notificaciones.dto.SolicitudNotificacionDTO;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.gestores.GestorNotificaciones;
import ar.edu.utn.frba.ddsi.notificaciones.models.repositories.RepositorioNotificaciones;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Recibe las notificaciones de la cola y las despacha.
 *
 * <p><b>La cola mezcla dos formas de mensaje y las distingue por un campo.</b> Llega
 * cualquiera de estas dos:
 *
 * <ul>
 *   <li>Un <b>aviso de entrega</b> con {@code id}: es una notificación que este mismo servicio
 *       ya guardó y solo falta enviar. Es lo que publica su propio productor interno.
 *   <li>Una <b>solicitud</b> sin {@code id}: es lo que mandan los servicios de dominio, que
 *       no conocen este esquema de base de datos y solo saben decir "mandá este mensaje a
 *       esta dirección".
 * </ul>
 *
 * <p>El servicio tiene que atender las dos: la primera porque es su propio ciclo de vida, la
 * segunda porque es el contrato con los demás servicios. Antes solo se atendía la primera y la
 * segunda se descartaba con un {@code ERROR} al ver el id nulo, con lo que la integración
 * entera no hacía nada.
 *
 * <p><b>Se lee como JSON crudo y no como un tipo fijo</b> justamente por eso: el mensaje
 * tiene dos formas distintas y el tipo de destino depende de cuál es. Con un DTO fijo, el
 * listener que espera el primero falla al deserializar el segundo, y el broker reintenta el
 * mismo mensaje para siempre.
 *
 * <p><b>El fallo se registra y no se relanza.</b> Si se relanzara, Spring devolvería el
 * mensaje a la cola y lo reintentaría en un ciclo cerrado. Para un mail o un WhatsApp que no
 * se puede entregar, reintentar cinco veces seguido es peor que dejarlo: la fila queda en
 * FALLIDA y se puede reintentar desde la base.
 */
@Component
public class ConsumidorNotificaciones {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorNotificaciones.class);

    private final GestorNotificaciones gestor;
    private final RepositorioNotificaciones repositorioNotificaciones;
    private final ObjectMapper objectMapper;

    public ConsumidorNotificaciones(GestorNotificaciones gestor,
                                    RepositorioNotificaciones repositorioNotificaciones,
                                    ObjectMapper objectMapper) {
        this.gestor = gestor;
        this.repositorioNotificaciones = repositorioNotificaciones;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = RabbitConfig.COLA_NOTIFICACIONES)
    public void recibir(Message mensaje) {
        String cuerpoCrudo = new String(mensaje.getBody(),
                java.nio.charset.StandardCharsets.UTF_8);
        if (cuerpoCrudo == null || cuerpoCrudo.isBlank()) {
            log.warn("LLEGA un mensaje vacío a la cola de notificaciones, se descarta");
            return;
        }

        try {
            JsonNode nodo = objectMapper.readTree(cuerpoCrudo);

            if (nodo.hasNonNull("id")) {
                procesarAvisoDeNotificacionExistente(cuerpoCrudo);
            } else {
                procesarSolicitud(cuerpoCrudo);
            }
        } catch (RuntimeException excepcion) {
            log.error("No se pudo procesar el mensaje de la cola: {}", excepcion.getMessage(),
                    excepcion);
        } catch (Exception errorDeLectura) {
            log.error("LLEGA un mensaje que no es JSON válido: {}", cuerpoCrudo, errorDeLectura);
        }
    }

    /**
     * Notificación ya guardada: este servicio la creó y solo falta despacharla.
     *
     * <p><b>Se busca por id y no se reenvía si ya salió.</b> Eso es lo que hace
     * idempotente al consumidor: si el broker redelivera (un reinicio a mitad del
     * procesamiento), el segundo intento encuentra la fila en ENVIADA y no manda un
     * segundo correo al donante.
     */
    private void procesarAvisoDeNotificacionExistente(String cuerpoCrudo) throws Exception {
        var mensaje = objectMapper.readValue(cuerpoCrudo, AvisoNotificacion.class);

        Optional<Notificacion> encontrada =
                repositorioNotificaciones.findById(UUID.fromString(mensaje.id()));

        if (encontrada.isEmpty()) {
            log.warn("La notificación {} no existe, el mensaje se descarta", mensaje.id());
            return;
        }

        Notificacion notificacion = encontrada.get();

        if (notificacion.getEstado()
                == ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion
                        .EstadoNotificacion.ENVIADA) {
            log.debug("La notificación {} ya estaba enviada, no se reenvía", notificacion.getId());
            return;
        }

        enviar(notificacion, mensaje.medioDeContacto(), mensaje.direccionDeContacto());
    }

    /**
     * Solicitud entrante de un servicio de dominio.
     *
     * <p>Se guarda primero y se publica para que el mismo servicio la despache por el
     * camino ya probado, en vez de duplicar ac la lógica de envío.
     */
    private void procesarSolicitud(String cuerpoCrudo) throws Exception {
        SolicitudNotificacionDTO solicitud =
                objectMapper.readValue(cuerpoCrudo, SolicitudNotificacionDTO.class);

        if (solicitud.getMedioDeContacto() == null || solicitud.getMedioDeContacto().isBlank()) {
            log.warn("LLEGA una solicitud sin medio de contacto, se descarta: {}", cuerpoCrudo);
            return;
        }

        if (solicitud.getDireccionDeContacto() == null || solicitud.getDireccionDeContacto().isBlank()) {
            log.warn("LLEGA una solicitud sin dirección de contacto, se descarta: {}", cuerpoCrudo);
            return;
        }

        gestor.enviarSolicitudDeNotificacion(
                solicitud.getMedioDeContacto(),
                solicitud.getDireccionDeContacto(),
                solicitud.getAsuntoMensaje(),
                solicitud.getCuerpoMensaje()
        );

        log.info("Solicitud de notificación encolada para {}",
                solicitud.getDireccionDeContacto());
    }

    private void enviar(Notificacion notificacion,
                        String medioDeContacto,
                        String direccionDeContacto) {
        try {
            gestor.enviarNotificacion(
                    medioDeContacto != null ? medioDeContacto : notificacion.getTipoMedioDeContacto(),
                    direccionDeContacto != null
                            ? direccionDeContacto
                            : notificacion.getDireccionDeContacto(),
                    notificacion
            );

            notificacion.marcarEnviada();
        } catch (RuntimeException excepcion) {
            notificacion.marcarFallida();

            log.error("No se pudo enviar la notificación {}: {}",
                    notificacion.getId(), excepcion.getMessage(), excepcion);
        }

        repositorioNotificaciones.save(notificacion);
    }

    /** El aviso de una notificación ya guardada, que es lo que publica el productor interno. */
    public record AvisoNotificacion(String id,
                                    String medioDeContacto,
                                    String direccionDeContacto) {}
}
