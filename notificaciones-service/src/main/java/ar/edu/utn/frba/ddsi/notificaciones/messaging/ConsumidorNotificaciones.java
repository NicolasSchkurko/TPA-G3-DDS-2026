package ar.edu.utn.frba.ddsi.notificaciones.messaging;

import ar.edu.utn.frba.ddsi.notificaciones.config.rabbit.RabbitConfig;
import ar.edu.utn.frba.ddsi.notificaciones.dto.SolicitudNotificacionDTO;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.EstadoNotificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.gestores.GestorNotificaciones;
import ar.edu.utn.frba.ddsi.notificaciones.models.repositories.RepositorioNotificaciones;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

/**
 * Consume la cola: distingue avisos (con {@code id}) de solicitudes (sin {@code id}). Los fallos se
 * registran y no se relanzan; lo que escape al listener termina en la dead letter.
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
        String cuerpoCrudo = new String(mensaje.getBody(), StandardCharsets.UTF_8);
        String routingKey = mensaje.getMessageProperties().getReceivedRoutingKey();

        log.info("[COLA] LLEGA mensaje a '{}' | routingKey={} | {} bytes | payload={}",
                RabbitConfig.COLA_NOTIFICACIONES, routingKey, mensaje.getBody().length,
                abreviatura(cuerpoCrudo));

        if (cuerpoCrudo.isBlank()) {
            log.warn("[COLA] Mensaje vacío, se descarta");
            return;
        }

        try {
            JsonNode nodo = objectMapper.readTree(cuerpoCrudo);

            if (nodo.hasNonNull("id")) {
                log.info("[COLA] Es un AVISO de notificación ya guardada (id={})", nodo.get("id").asText());
                procesarAvisoDeNotificacionExistente(cuerpoCrudo);
            } else {
                log.info("[COLA] Es una SOLICITUD de notificación de otro servicio");
                procesarSolicitud(cuerpoCrudo);
            }
        } catch (RuntimeException excepcion) {
            log.error("[COLA] No se pudo procesar el mensaje: {}", excepcion.getMessage(), excepcion);
        } catch (Exception errorDeLectura) {
            log.error("[COLA] Mensaje que no es JSON válido: {}", cuerpoCrudo, errorDeLectura);
        }
    }

    /** Recorta el payload para no llenar el log; el mensaje completo ya está en JSON. */
    private static String abreviatura(String texto) {
        return texto.length() <= 300 ? texto : texto.substring(0, 300) + "...";
    }

    /** Aviso de una notificación ya guardada: la busca y la despacha; no la reenvía si ya salió. */
    private void procesarAvisoDeNotificacionExistente(String cuerpoCrudo) throws Exception {
        var mensaje = objectMapper.readValue(cuerpoCrudo, AvisoNotificacion.class);

        log.info("[COLA] Aviso id={}: buscando la notificación en la base", mensaje.id());

        Optional<Notificacion> encontrada =
                repositorioNotificaciones.findById(UUID.fromString(mensaje.id()));

        if (encontrada.isEmpty()) {
            log.warn("[COLA] La notificación {} no existe, el aviso se descarta", mensaje.id());
            return;
        }

        Notificacion notificacion = encontrada.get();

        if (notificacion.getEstado() == EstadoNotificacion.ENVIADA) {
            log.info("[COLA] La notificación {} ya estaba enviada, no se reenvía", notificacion.getId());
            return;
        }

        log.info("[COLA] Despachando notificación {} (medio='{}', destino='{}')",
                notificacion.getId(), mensaje.medioDeContacto(), mensaje.direccionDeContacto());

        enviar(notificacion, mensaje.medioDeContacto(), mensaje.direccionDeContacto());
    }

    /** Solicitud de un servicio de dominio: la guarda y la encola por el camino normal. */
    private void procesarSolicitud(String cuerpoCrudo) throws Exception {
        SolicitudNotificacionDTO solicitud =
                objectMapper.readValue(cuerpoCrudo, SolicitudNotificacionDTO.class);

        log.info("[COLA] Solicitud parseada: medio='{}', destino='{}', asunto='{}'",
                solicitud.getMedioDeContacto(), solicitud.getDireccionDeContacto(),
                solicitud.getAsuntoMensaje());

        if (solicitud.getMedioDeContacto() == null || solicitud.getMedioDeContacto().isBlank()) {
            log.warn("[COLA] Solicitud sin medio de contacto, se descarta: {}", abreviatura(cuerpoCrudo));
            return;
        }

        if (solicitud.getDireccionDeContacto() == null || solicitud.getDireccionDeContacto().isBlank()) {
            log.warn("[COLA] Solicitud sin dirección de contacto, se descarta: {}", abreviatura(cuerpoCrudo));
            return;
        }

        gestor.enviarSolicitudDeNotificacion(
                solicitud.getMedioDeContacto(),
                solicitud.getDireccionDeContacto(),
                solicitud.getAsuntoMensaje(),
                solicitud.getCuerpoMensaje()
        );

        log.info("[COLA] Solicitud guardada y encolada para '{}'", solicitud.getDireccionDeContacto());
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
            log.info("[COLA] Notificación {} ENVIADA (estado={})",
                    notificacion.getId(), notificacion.getEstado());
        } catch (RuntimeException excepcion) {
            notificacion.marcarFallida();

            log.error("[COLA] Notificación {} FALLÓ (estado={}): {}",
                    notificacion.getId(), notificacion.getEstado(),
                    excepcion.getMessage(), excepcion);
        }

        repositorioNotificaciones.save(notificacion);
    }

    /** El aviso de una notificación ya guardada, que es lo que publica el productor interno. */
    public record AvisoNotificacion(String id,
                                    String medioDeContacto,
                                    String direccionDeContacto) {}
}
