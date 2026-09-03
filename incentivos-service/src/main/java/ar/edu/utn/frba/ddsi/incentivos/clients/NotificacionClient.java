package ar.edu.utn.frba.ddsi.incentivos.clients;

import ar.edu.utn.frba.ddsi.incentivos.dto.Notificaciones.PerfilNotificacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.EnvioNotificacionException;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mensaje.MedioContacto;
import ar.edu.utn.frba.ddsi.incentivos.models.events.CategoriaNuevaPublicar;
import ar.edu.utn.frba.ddsi.incentivos.models.events.MisionCambiada;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioNotificacionesPendientes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class NotificacionClient {
    @Value("${servicio.notificaciones.url}")
    private String notificacionesUrl;

    private final RestTemplate restTemplate;
    private final RepositorioNotificacionesPendientes repositorioPendientes;

    public NotificacionClient(RestTemplate restTemplate,
                              RepositorioNotificacionesPendientes repositorioPendientes) {
        this.restTemplate = restTemplate;
        this.repositorioPendientes = repositorioPendientes;
    }

    public void enviarNotificacion(PerfilNotificacionDTO dto) throws EnvioNotificacionException {
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

    @EventListener
    public void notificarCambioMision(MisionCambiada event) {
        enviar(event.contacto(),
               "Nueva misión disponible",
               crearMensajeMision(event.misionAnterior(), event.misionNueva()));
    }

    @EventListener
    public void notificarCambioCategoria(CategoriaNuevaPublicar event) {
        enviar(event.contacto(),
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

    private String crearMensajeCategoria(String categoriaAnterior, String categoriaNueva) {
        return "Completaste la categoría '%s' y avanzaste a '%s'.".formatted(categoriaAnterior, categoriaNueva);
    }
}