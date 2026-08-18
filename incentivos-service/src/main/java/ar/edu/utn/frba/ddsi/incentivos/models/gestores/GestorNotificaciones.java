package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.clients.NotificacionClient;
import ar.edu.utn.frba.ddsi.incentivos.dto.PerfilNotificacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.events.CategoriaCambiada;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.events.MisionCambiada;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mensaje.MedioContacto;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GestorNotificaciones {
    private final GestorImpacto gestorImpacto;
    private final NotificacionClient notificacionClient;

    public GestorNotificaciones(GestorImpacto gestorImpacto, NotificacionClient notificacionClient) {
        this.gestorImpacto = gestorImpacto;
        this.notificacionClient = notificacionClient;
    }

    @EventListener
    public void notificarCambioMision(MisionCambiada event) {
        enviar(event.idUsuario(), "Nueva mision disponible", "Completaste '" + event.misionAnterior()
                + "'. Tu nueva mision es '" + event.misionNueva() + "'.");
    }

    @EventListener
    public void notificarCambioCategoria(CategoriaCambiada event) {
        enviar(event.idUsuario(), "Nueva categoria", "Completaste la categoria '" + event.categoriaAnterior()
                + "' y avanzaste a '" + event.categoriaNueva() + "'.");
    }

    private void enviar(UUID idUsuario, String asunto, String cuerpo) {
        MedioContacto contacto = gestorImpacto.obtenerContacto(idUsuario);
        if (contacto == null) return;
        notificacionClient.enviarNotificacion(new PerfilNotificacionDTO(
                contacto.getMedioDeContacto(), contacto.getDireccionContacto(), cuerpo, asunto));
    }
}
