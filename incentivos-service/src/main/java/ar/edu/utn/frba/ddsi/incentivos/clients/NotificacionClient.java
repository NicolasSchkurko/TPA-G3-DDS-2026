package ar.edu.utn.frba.ddsi.incentivos.clients;

import ar.edu.utn.frba.ddsi.incentivos.config.RabbitMQConfig;
import ar.edu.utn.frba.ddsi.incentivos.dto.Notificaciones.PerfilNotificacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.EnvioNotificacionException;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mensaje.MedioContacto;
import ar.edu.utn.frba.ddsi.incentivos.models.events.CategoriaNuevaPublicar;
import ar.edu.utn.frba.ddsi.incentivos.models.events.MisionCambiada;
import ar.edu.utn.frba.ddsi.incentivos.models.events.MisionCompletada;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioNotificacionesPendientes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publica las notificaciones del donante en el broker.
 *
 * <p><b>Va por Rabbit y no por HTTP, y es lo que pide el enunciado.</b> Dice textualmente
 * que la integración con el servicio de notificaciones "deberá realizarse de forma asíncrona, a
 * través de una cola de mensajes, a fin de no afectar la disponibilidad del sistema ante picos
 * de carga o fallas transitorias".
 *
 * <p>Antes era un {@code restTemplate.postForEntity} dentro de un
 * {@code @TransactionalEventListener}, o sea **sincrónico y bloqueante**: si el servicio de
 * notificaciones tardaba o estaba caído, el hilo quedaba esperando y, como la llamada ocurre
 * en {@code AFTER_COMMIT}, la transacción ya estaba commiteada pero la excepción subía igual.
 * Con un pico de notificaciones, el servicio entero se caía por culpa del receptor.
 *
 * <p><b>Si el broker falla, la notificación queda en pendientes y el error sube.</b> Publicar
 * es barato y no debe romper la operación de dominio que la originó, pero perder el aviso
 * silenciosamente es peor: por eso se guarda en el buffer de pendientes antes de propagar
 * {@link EnvioNotificacionException}, que el llamador ya usa para saber que el aviso no salió.
 *
 * <p><b>El exchange lo declara este servicio</b> y notificaciones ata su cola a él. La frontera
 * va del lado del que publica, que es lo que evita que cada consumidor tenga que conocer el
 * nombre de las colas de los demás.
 */
@Slf4j
@Service
public class NotificacionClient {

    private final RabbitTemplate rabbitTemplate;
    private final DonacionClient donacionClient;
    private final RepositorioNotificacionesPendientes repositorioPendientes;

    public NotificacionClient(RabbitTemplate rabbitTemplate,
                              DonacionClient donacionClient,
                              RepositorioNotificacionesPendientes repositorioPendientes) {
        this.rabbitTemplate = rabbitTemplate;
        this.donacionClient = donacionClient;
        this.repositorioPendientes = repositorioPendientes;
    }

    /**
     * Publica la notificación en el exchange de notificaciones.
     *
     * @throws EnvioNotificacionException si el broker no acepta el mensaje; en ese caso la
     *                                   notificación queda guardada como pendiente
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

            repositorioPendientes.guardar(dto);
            throw new EnvioNotificacionException(dto);
        }
    }

    /** Le avisa al donante que terminó una misión y qué insignia ganó. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notificarMisionCompletada(MisionCompletada event) {
        enviar(
                donacionClient.obtenerContactoPersona(event.idUsuario()),
                "¡Misión completada!",
                crearMensajeMisionCompletada(
                        event.misionAnterior(),
                        event.insigniaObtenida(),
                        event.impactoDonacion().getEntidadBeneficiaria()
                )
        );
    }

    /** Le avisa al donante que tiene una misión nueva disponible. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notificarCambioMision(MisionCambiada event) {
        enviar(donacionClient.obtenerContactoPersona(event.idUsuario()),
                "Nueva misión disponible",
                crearMensajeMision(event.misionAnterior(), event.misionNueva()));
    }

    /** Le avisa al donante que avanzó de categoría. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notificarCambioCategoria(CategoriaNuevaPublicar event) {
        enviar(donacionClient.obtenerContactoPersona(event.idUsuario()),
                "Nueva categoría",
                crearMensajeCategoria(event.categoriaAnterior(), event.categoriaNueva()));
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
            log.error("Falló el envío de notificación '{}' - se guardó en pendientes", asunto);
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