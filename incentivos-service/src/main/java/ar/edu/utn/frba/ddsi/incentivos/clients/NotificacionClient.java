package ar.edu.utn.frba.ddsi.incentivos.clients;

import ar.edu.utn.frba.ddsi.incentivos.config.RabbitMQConfig;
import ar.edu.utn.frba.ddsi.incentivos.dto.Notificaciones.PerfilNotificacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.EnvioNotificacionException;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mensaje.MedioContacto;
import ar.edu.utn.frba.ddsi.incentivos.models.events.CategoriaNuevaPublicar;
import ar.edu.utn.frba.ddsi.incentivos.models.events.MisionCambiada;
import ar.edu.utn.frba.ddsi.incentivos.models.events.MisionCompletada;
import ar.edu.utn.frba.ddsi.incentivos.services.NotificacionesPendientesService;
import ar.edu.utn.frba.ddsi.incentivos.services.NotificacionesPendientesService.NotificacionReclamada;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publica las notificaciones del donante en el broker, de forma asíncrona. Si el broker falla,
 * la notificación queda en pendientes y se propaga {@link EnvioNotificacionException}.
 */
@Slf4j
@Service
public class NotificacionClient {

    private static final int MAX_POR_CICLO = 50;

    private final RabbitTemplate rabbitTemplate;
    private final DonacionClient donacionClient;
    private final NotificacionesPendientesService pendientesService;
    private final TaskExecutor notificacionExecutor;

    public NotificacionClient(RabbitTemplate rabbitTemplate,
                              DonacionClient donacionClient,
                              NotificacionesPendientesService pendientesService,
                              @Qualifier("notificacionExecutor")
                              TaskExecutor notificacionExecutor) {
        this.rabbitTemplate = rabbitTemplate;
        this.donacionClient = donacionClient;
        this.pendientesService = pendientesService;
        this.notificacionExecutor = notificacionExecutor;
    }

    /**
     * Publica la notificación en el exchange.
     *
     * @throws EnvioNotificacionException si el broker no acepta el mensaje; queda pendiente
     */
    public void enviarNotificacion(PerfilNotificacionDTO dto) throws EnvioNotificacionException {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NOTIFICACIONES,
                    RabbitMQConfig.RK_INCENTIVO,
                    dto);

            log.info("Notificación publicada para {}", dto.getDireccionDeContacto());
        } catch (Exception e) {
            log.error("Error al publicar la notificación para {}, guardando en pendientes",
                    dto.getDireccionDeContacto(), e);

            pendientesService.encolar(dto);
            throw new EnvioNotificacionException(dto);
        }
    }

    /** Le avisa al donante que terminó una misión y qué insignia ganó. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notificarMisionCompletada(MisionCompletada event) {
        notificacionExecutor.execute(() -> enviar(
                donacionClient.obtenerContactoPersona(event.idUsuario()),
                "¡Misión completada!",
                crearMensajeMisionCompletada(
                        event.misionAnterior(),
                        event.insigniaObtenida(),
                        event.impactoDonacion().getEntidadBeneficiaria()
                )
        ));
    }

    /** Le avisa al donante que tiene una misión nueva disponible. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notificarCambioMision(MisionCambiada event) {
        notificacionExecutor.execute(() ->
                enviar(donacionClient.obtenerContactoPersona(event.idUsuario()),
                        "Nueva misión disponible",
                        crearMensajeMision(event.misionAnterior(), event.misionNueva())));
    }

    /** Le avisa al donante que avanzó de categoría. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notificarCambioCategoria(CategoriaNuevaPublicar event) {
        notificacionExecutor.execute(() ->
                enviar(donacionClient.obtenerContactoPersona(event.idUsuario()),
                        "Nueva categoría",
                        crearMensajeCategoria(event.categoriaAnterior(), event.categoriaNueva())));
    }

    /**
     * Reintenta las notificaciones que quedaron en la outbox. Publica fuera de la transacción
     * de reclamo, igual que la outbox de n8n.
     */
    @Scheduled(
            fixedDelayString = "${servicio.notificaciones.outbox.intervalo-reintento-ms:30000}",
            initialDelayString = "${servicio.notificaciones.outbox.retraso-inicial-ms:10000}"
    )
    public void procesarNotificacionesPendientes() {
        for (int procesadas = 0; procesadas < MAX_POR_CICLO; procesadas++) {
            Optional<NotificacionReclamada> siguiente =
                    pendientesService.reclamarSiguiente(LocalDateTime.now());
            if (siguiente.isEmpty()) {
                return;
            }

            NotificacionReclamada reclamada = siguiente.orElseThrow();
            try {
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.EXCHANGE_NOTIFICACIONES,
                        RabbitMQConfig.RK_INCENTIVO,
                        reclamada.contenido());
                pendientesService.confirmar(reclamada.id());
                log.info("Notificación pendiente {} publicada en el intento {}",
                        reclamada.id(), reclamada.intentos());
            } catch (Exception e) {
                pendientesService.reintentar(reclamada.id(), LocalDateTime.now(),
                        e.getMessage());
                log.warn("No se pudo publicar la notificación pendiente {} en el intento {}; "
                                + "se reintentará: {}",
                        reclamada.id(), reclamada.intentos(), e.getMessage());
            }
        }
    }

    private void enviar(MedioContacto contacto, String asunto, String cuerpo) {
        if (contacto == null) {
            log.warn("Intento de envío con contacto nulo");
            return;
        }

        try {
            this.enviarNotificacion(
                    new PerfilNotificacionDTO(
                            contacto.getMedioDeContacto(),
                            contacto.getDireccionContacto(),
                            cuerpo,
                            asunto
                    )
            );
        } catch (EnvioNotificacionException e) {
            log.debug("La notificación '{}' quedó en la outbox de notificaciones", asunto);
        }
    }

    private String crearMensajeMision(String misionAnterior, String misionNueva) {
        return "Completaste '%s'. Tu nueva misión es '%s'.".formatted(misionAnterior, misionNueva);
    }

    private String crearMensajeMisionCompletada(String misionAnterior,
                                                String insigniaObtenida,
                                                String entidadBeneficiaria) {
        return "Completaste '%s' y obtuviste la insignia '%s' tras impactar a '%s'."
                .formatted(misionAnterior, insigniaObtenida, entidadBeneficiaria);
    }

    private String crearMensajeCategoria(String categoriaAnterior, String categoriaNueva) {
        return "Completaste la categoría '%s' y avanzaste a '%s'."
                .formatted(categoriaAnterior, categoriaNueva);
    }
}