package ar.edu.utn.frba.ddsi.incentivos.clients;

import ar.edu.utn.frba.ddsi.incentivos.dto.Notificaciones.PerfilNotificacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.EnvioNotificacionException;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mensaje.MedioContacto;
import ar.edu.utn.frba.ddsi.incentivos.models.events.CategoriaNuevaPublicar;
import ar.edu.utn.frba.ddsi.incentivos.models.events.MisionCambiada;
import ar.edu.utn.frba.ddsi.incentivos.models.events.MisionCompletada;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioNotificacionesPendientes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestTemplate;

/**
 * Avisa a {@code notificaciones-service}, y escucha los eventos del servicio para
 * notificarle al donante lo que le pasa.
 *
 * <p>Los tres listeners corren en {@code AFTER_COMMIT} a propósito: notificar antes de que
 * la transacción confirme puede avisarle al donante de una insignia que después no se
 * guardó.
 */
@Slf4j
@Service
public class NotificacionClient {
    @Value("${servicio.notificaciones.url}")
    private String notificacionesUrl;

    private final RestTemplate restTemplate;
    private final DonacionClient donacionClient;

    private final RepositorioNotificacionesPendientes repositorioPendientes;

    public NotificacionClient(RestTemplate restTemplate,
                              DonacionClient donacionClient,
                              RepositorioNotificacionesPendientes repositorioPendientes) {
        this.restTemplate = restTemplate;
        this.donacionClient = donacionClient;
        this.repositorioPendientes = repositorioPendientes;
    }

    /**
     * Manda la notificación y, si falla, la guarda en la cola de pendientes.
     *
     * @throws EnvioNotificacionException si el envío falla. Quien llama desde un listener
     *                                    lo captura y solo loguea, porque el error ya
     *                                    quedó registrado en la cola de pendientes.
     */
    public void enviarNotificacion(PerfilNotificacionDTO dto)
            throws EnvioNotificacionException {
        try {
            restTemplate.postForEntity(notificacionesUrl, dto, void.class);
            log.info("Notificación enviada exitosamente a {}", dto.getDireccionContacto());
        } catch (Exception e) {
            log.error("Error al enviar notificación a {}, guardando en pendientes",
                     dto.getDireccionContacto(), e);
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

    /**
     * El contacto se resuelve <b>aca</b>, y no cuando se armo el evento (punto 12).
     *
     * <p>Este listener corre en {@code AFTER_COMMIT}: la transaccion ya cerro y la conexion
     * del pool esta liberada. Antes el contacto se pedia dentro de la transaccion, solo para
     * guardarlo en el evento y usarlo aca, o sea que se retenia una conexion durante una
     * llamada HTTP sin usar el resultado hasta mucho despues.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notificarCambioMision(MisionCambiada event) {
        enviar(donacionClient.obtenerContactoPersona(event.idUsuario()),
               "Nueva misión disponible",
               crearMensajeMision(event.misionAnterior(), event.misionNueva()));
    }

    /**
     * Le avisa al donante que subió de categoría, contando desde cuál venía.
     *
     * <p>El contacto se resuelve acá y no en el evento, igual que en
     * {@link #notificarMisionCompletada}: esta transacción ya cerró.
     */
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
        return "Completaste la categoría '%s' y avanzaste a '%s'.".formatted(categoriaAnterior, categoriaNueva);
    }
}
