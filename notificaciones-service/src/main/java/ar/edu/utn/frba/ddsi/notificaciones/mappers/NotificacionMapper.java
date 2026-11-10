package ar.edu.utn.frba.ddsi.notificaciones.mappers;

import ar.edu.utn.frba.ddsi.notificaciones.dto.Notificaciones.NotificacionDTO;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class NotificacionMapper {
    public NotificacionDTO notificacionDTO(Notificacion notificacion) {
        NotificacionDTO notificacionDTO = new NotificacionDTO();
        notificacionDTO.setId(Objects.toString(notificacion.getId(), null));

        // La relación mensaje y las columnas de auditoría pueden venir nulas en filas anómalas o
        // entidades construidas sin argumentos: se mapea null en vez de romper el GET con un NPE.
        Mensaje mensaje = notificacion.getMensaje();
        if (mensaje != null) {
            notificacionDTO.setAsunto(mensaje.getAsunto());
            notificacionDTO.setCuerpo(mensaje.getCuerpo());
            notificacionDTO.setUrlMapa(mensaje.getUrlMapa());
        }
        notificacionDTO.setDireccionDeContacto(notificacion.getDireccionDeContacto());
        notificacionDTO.setTipoMedioDeContacto(notificacion.getTipoMedioDeContacto());
        notificacionDTO.setEstado(Objects.toString(notificacion.getEstado(), null));
        notificacionDTO.setFechaCreacion(Objects.toString(notificacion.getFechaCreacion(), null));
        if (notificacion.getFechaEnvio() != null) {
            notificacionDTO.setFechaEnvio(notificacion.getFechaEnvio().toString());
        }

        return notificacionDTO;
    }
}
