package ar.edu.utn.frba.ddsi.incentivos.models.entities.ServicioNotificaciones;

import ar.edu.utn.frba.ddsi.incentivos.clients.NotificacionClient;
import ar.edu.utn.frba.ddsi.incentivos.dto.MedioContactoDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.PerfilNotificacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mensaje.Mensaje;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Obtiene el medio o los medios de contacto, hace las validaciones necesarias,
 * crea el NotificacionesDTO, y llama NotificacioneClient
 */

@Service
public class ServicioNotificaciones {

  private final NotificacionClient notificacionesClient;

  public ServicioNotificaciones(NotificacionClient notificacionesClient) {
    this.notificacionesClient = notificacionesClient;
  }

  public void enviarNotificacion(MedioContactoDTO medioDeContacto, Mensaje mensaje) {
    PerfilNotificacionDTO dto = new PerfilNotificacionDTO(
        medioDeContacto.getMedioDeContacto(),
        medioDeContacto.getDireccionContacto(),
        mensaje.getCuerpo(),
        mensaje.getAsunto()
    );

    notificacionesClient.enviarNotificacion(dto);
  }

}
