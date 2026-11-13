package ar.edu.utn.frba.ddsi.notificaciones.messaging;

import ar.edu.utn.frba.ddsi.notificaciones.config.rabbit.RabbitConfig;
import ar.edu.utn.frba.ddsi.notificaciones.dto.Notificaciones.SolicitudNotificacionDTO;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import ar.edu.utn.frba.ddsi.notificaciones.services.NotificadorService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

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

    private final NotificadorService notificadorService;
    private final ObjectMapper objectMapper;

    public ConsumidorNotificaciones(NotificadorService notificadorService,
                                    ObjectMapper objectMapper) {
        this.notificadorService = notificadorService;
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

        Optional<Notificacion> reclamada = notificadorService.reclamarParaEnvio(
                uuid, mensaje.medioDeContacto(), mensaje.direccionDeContacto());

        if (reclamada.isEmpty()) {
            if (notificadorService.obtenerPorId(uuid).isEmpty()) {
                log.warn("[COLA] La notificación {} no existe, el aviso se rechaza a la dead letter", mensaje.id());
                throw new AmqpRejectAndDontRequeueException("Notificación con id " + mensaje.id() + " no encontrada");
            }
            log.info("[COLA] La notificación {} ya estaba enviada o en curso, no se reenvía", uuid);
            return;
        }

        Notificacion notificacion = reclamada.get();
        log.info("[COLA] Despachando notificación {} (medio='{}', destino='{}')",
                notificacion.getId(), notificacion.getTipoMedioDeContacto(),
                notificacion.getDireccionDeContacto());

        enviar(notificacion);
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

        if (solicitud.getUrlMapa() != null && solicitud.getUrlMapa().length() > 1024) {
            log.warn("[COLA] Solicitud con urlMapa demasiado largo, se rechaza a la dead letter");
            throw new AmqpRejectAndDontRequeueException("Solicitud con urlMapa de más de 1024 caracteres");
        }

        notificadorService.procesarSolicitudDeNotificacion(solicitud);

        log.info("[COLA] Solicitud guardada y encolada para '{}'", solicitud.getDireccionDeContacto());
    }

    private void enviar(Notificacion notificacion) {
        UUID claimId = notificacion.getClaimId();

        try {
            notificadorService.enviarNotificacion(notificacion,
                    notificacion.getTipoMedioDeContacto(), notificacion.getDireccionDeContacto());
            boolean cerrada = notificadorService.marcarEnviada(notificacion.getId(), claimId);
            if (cerrada) {
                log.info("[COLA] Notificación {} ENVIADA", notificacion.getId());
            } else {
                log.warn("[COLA] La notificación {} salió a n8n pero el claim {} ya no era el vigente",
                        notificacion.getId(), claimId);
            }
        } catch (HttpClientErrorException error4xx) {
            marcarFallida(notificacion, claimId);
            log.error("[COLA] Notificación {} FALLÓ (4xx de n8n): {}",
                    notificacion.getId(), error4xx.getMessage(), error4xx);
            if (esTransitorio(error4xx.getStatusCode())) {
                // 408/425/429: n8n está limitando o tardando; el retry de AMQP puede salvarlo.
                throw error4xx;
            }
            throw new AmqpRejectAndDontRequeueException("n8n devolvió un 4xx permanente", error4xx);
        } catch (IllegalArgumentException datoInvalido) {
            // Medio de contacto desconocido: dato inválido, no hay nada que reintentar.
            marcarFallida(notificacion, claimId);
            log.error("[COLA] Notificación {} FALLÓ (medio de contacto inválido): {}",
                    notificacion.getId(), datoInvalido.getMessage(), datoInvalido);
            throw new AmqpRejectAndDontRequeueException("Medio de contacto inválido", datoInvalido);
        } catch (RestClientException errorTransitorio) {
            // Red/timeout/5xx: se relanza para que actúen los reintentos de Spring AMQP y la DLQ.
            marcarFallida(notificacion, claimId);
            log.error("[COLA] Notificación {} FALLÓ (falla transitoria de n8n): {}",
                    notificacion.getId(), errorTransitorio.getMessage(), errorTransitorio);
            throw errorTransitorio;
        } catch (RuntimeException inesperado) {
            marcarFallida(notificacion, claimId);
            log.error("[COLA] Notificación {} FALLÓ (error inesperado): {}",
                    notificacion.getId(), inesperado.getMessage(), inesperado);
            throw inesperado;
        }
    }

    private void marcarFallida(Notificacion notificacion, UUID claimId) {
        if (!notificadorService.marcarFallida(notificacion.getId(), claimId)) {
            log.warn("[COLA] El claim {} de la notificación {} ya no era el vigente",
                    claimId, notificacion.getId());
        }
    }

    private static boolean esTransitorio(HttpStatusCode status) {
        return status.value() == 408 || status.value() == 425 || status.value() == 429;
    }

    /** El aviso de una notificación ya guardada, que es lo que publica el productor interno. */
    public record AvisoNotificacion(String id,
                                    String medioDeContacto,
                                    String direccionDeContacto) {}
}
