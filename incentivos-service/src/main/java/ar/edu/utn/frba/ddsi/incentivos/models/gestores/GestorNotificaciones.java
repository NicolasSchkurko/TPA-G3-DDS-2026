package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.clients.DonacionClient;
import ar.edu.utn.frba.ddsi.incentivos.clients.NotificacionClient;
import ar.edu.utn.frba.ddsi.incentivos.dto.PerfilNotificacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.events.CategoriaNuevaPublicar;
import ar.edu.utn.frba.ddsi.incentivos.models.events.MisionCambiada;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mensaje.MedioContacto;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioNotificacionesPendientes;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GestorNotificaciones {
    private final RepositorioNotificacionesPendientes repositorio;
    private final NotificacionClient notificacionClient;
    private final DonacionClient cliente;
    //en un futuro manejare ese repo con una excepcion en notificacionClient

    public GestorNotificaciones(RepositorioNotificacionesPendientes repositorio,
                                NotificacionClient notificacionClient,
                                DonacionClient cliente) {
        this.repositorio = repositorio;
        this.notificacionClient = notificacionClient;
        this.cliente = cliente;
    }

    @EventListener
    public void notificarCambioMision(MisionCambiada event) {
        enviar(event.idUsuario(),
                "Nueva mision disponible",
                "Completaste '" + event.misionAnterior()
                + "'. Tu nueva mision es '" + event.misionNueva() + "'.");
    }

    @EventListener
    public void notificarCambioCategoria(CategoriaNuevaPublicar event) {
        enviar(event.idUsuario(),
                "Nueva categoria",
                "Completaste la categoria '" + event.categoriaAnterior()
                + "' y avanzaste a '" + event.categoriaNueva() + "'.");
    }

    private void enviar(UUID idUsuario, String asunto, String cuerpo) {
        MedioContacto contacto = cliente.obtenerContactoPersona(idUsuario);
        if (contacto == null) return;
        notificacionClient.enviarNotificacion(
                new PerfilNotificacionDTO(
                        contacto.getMedioDeContacto(),
                        contacto.getDireccionContacto(),
                        cuerpo,
                        asunto
                )
        );
    }
}
