package ar.edu.utn.frba.ddsi.notificaciones.messaging;

import ar.edu.utn.frba.ddsi.notificaciones.config.rabbit.RabbitConfig;
import ar.edu.utn.frba.ddsi.notificaciones.dto.SolicitudNotificacionDTO;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.EstadoNotificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.gestores.GestorNotificaciones;
import ar.edu.utn.frba.ddsi.notificaciones.models.repositories.RepositorioNotificaciones;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

/**
 * Consume la cola: distingue avisos (con {@code id}) de solicitudes (sin {@code id}).
 * Los errores de datos no recuperables se derivan a la dead letter; los fallos transitorios
 * se relanzan para activar el reintento configurado en Spring AMQP.
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
        String routingKey = mensaje.getMessageProperties() != null ? mensaje.getMessageProperties().getReceivedRoutingKey() : null;

        log.info("[COLA] LLEGA mensaje a '{}' | routingKey={} | {} bytes | payload={}",
                RabbitConfig.COLA_NOTIFICACIONES, routingKey, mensaje.getBody().length,
                abreviatura(cuerpoCrudo));

        if (cuerpoCrudo.isBlank()) {
            log.warn("[COLA] Mensaje vacío, se rechaza a la dead letter");
            throw new AmqpRejectAndDontRequeueException("Mensaje vacío recibido en " + RabbitConfig.COLA_NOTIFICACIONES);
        }

        try {
            if (RabbitConfig.RK_AVISO.equals(routingKey)) {
                log.info("[COLA] Es un AVISO de notificación ya guardada (rk={})", routingKey);
                procesarAvisoDeNotificacionExistente(cuerpoCrudo);
            } else {
                log.info("[COLA] Es una SOLICITUD de notificación de otro servicio (rk={})", routingKey);
                procesarSolicitud(cuerpoCrudo);
            }
        } catch (AmqpRejectAndDontRequeueException rechazoDirecto) {
            // Error irremediable: va a la DLQ sin reintentar
            throw rechazoDirecto;
        } catch (Exception error) {
            log.error("[COLA] Error al procesar mensaje en '{}': {}",
                    RabbitConfig.COLA_NOTIFICACIONES, error.getMessage(), error);
            // Error transitorio (e.g. BD): relanzar para que actúen los reintentos de Spring AMQP y la DLQ final
            throw error instanceof RuntimeException re ? re : new RuntimeException(error);
        }
    }

    /** Recorta el payload para no llenar el log; el mensaje completo ya está en JSON. */
    private static String abreviatura(String texto) {
        return texto.length() <= 300 ? texto : texto.substring(0, 300) + "...";
    }

    /** Aviso de una notificación ya guardada: la busca y la despacha; no la reenvía si ya salió. */
    private void procesarAvisoDeNotificacionExistente(String cuerpoCrudo) throws Exception {
        AvisoNotificacion mensaje;
        try {
            mensaje = objectMapper.readValue(cuerpoCrudo, AvisoNotificacion.class);
        } catch (Exception e) {
            log.warn("[COLA] Aviso con JSON no deserializable: {}", abreviatura(cuerpoCrudo), e);
            throw new AmqpRejectAndDontRequeueException("JSON no deserializable a AvisoNotificacion", e);
        }

        log.info("[COLA] Aviso id={}: buscando la notificación en la base", mensaje.id());

        UUID uuid;
        try {
            uuid = UUID.fromString(mensaje.id());
        } catch (Exception e) {
            log.warn("[COLA] Aviso con id no válido como UUID: {}", mensaje.id());
            throw new AmqpRejectAndDontRequeueException("Aviso con id no UUID: " + mensaje.id(), e);
        }

        Optional<Notificacion> encontrada = repositorioNotificaciones.findById(uuid);

        if (encontrada.isEmpty()) {
            log.warn("[COLA] La notificación {} no existe, el aviso se rechaza a la dead letter", mensaje.id());
            throw new AmqpRejectAndDontRequeueException("Notificación con id " + mensaje.id() + " no encontrada");
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

    /** Solicitud de un servicio de dominio: valida los 4 campos y la guarda/encola. */
    private void procesarSolicitud(String cuerpoCrudo) throws Exception {
        SolicitudNotificacionDTO solicitud;
        try {
            solicitud = objectMapper.readValue(cuerpoCrudo, SolicitudNotificacionDTO.class);
        } catch (Exception e) {
            log.warn("[COLA] Solicitud con JSON no deserializable: {}", abreviatura(cuerpoCrudo), e);
            throw new AmqpRejectAndDontRequeueException("JSON no deserializable a SolicitudNotificacionDTO", e);
        }

        log.info("[COLA] Solicitud parseada: idSolicitud='{}', medio='{}', destino='{}', asunto='{}'",
                solicitud.getIdSolicitud(),
                solicitud.getMedioDeContacto(), solicitud.getDireccionDeContacto(),
                solicitud.getAsuntoMensaje());

        if (solicitud.getIdSolicitud() != null && !solicitud.getIdSolicitud().isBlank()) {
            if (repositorioNotificaciones.findByIdSolicitud(solicitud.getIdSolicitud()).isPresent()) {
                log.info("[COLA] Solicitud idSolicitud='{}' ya procesada previamente, se descarta por idempotencia",
                        solicitud.getIdSolicitud());
                return;
            }
        }

        if (solicitud.getMedioDeContacto() == null || solicitud.getMedioDeContacto().isBlank()) {
            log.warn("[COLA] Solicitud sin medio de contacto, se rechaza a la dead letter: {}", abreviatura(cuerpoCrudo));
            throw new AmqpRejectAndDontRequeueException("Solicitud sin medioDeContacto obligatorio");
        }

        if (solicitud.getDireccionDeContacto() == null || solicitud.getDireccionDeContacto().isBlank()) {
            log.warn("[COLA] Solicitud sin dirección de contacto, se rechaza a la dead letter: {}", abreviatura(cuerpoCrudo));
            throw new AmqpRejectAndDontRequeueException("Solicitud sin direccionDeContacto obligatoria");
        }

        if (solicitud.getAsuntoMensaje() == null || solicitud.getAsuntoMensaje().isBlank()) {
            log.warn("[COLA] Solicitud sin asunto de mensaje, se rechaza a la dead letter: {}", abreviatura(cuerpoCrudo));
            throw new AmqpRejectAndDontRequeueException("Solicitud sin asuntoMensaje obligatorio");
        }

        if (solicitud.getCuerpoMensaje() == null || solicitud.getCuerpoMensaje().isBlank()) {
            log.warn("[COLA] Solicitud sin cuerpo de mensaje, se rechaza a la dead letter: {}", abreviatura(cuerpoCrudo));
            throw new AmqpRejectAndDontRequeueException("Solicitud sin cuerpoMensaje obligatorio");
        }

        gestor.enviarSolicitudDeNotificacion(
                solicitud.getIdSolicitud(),
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
