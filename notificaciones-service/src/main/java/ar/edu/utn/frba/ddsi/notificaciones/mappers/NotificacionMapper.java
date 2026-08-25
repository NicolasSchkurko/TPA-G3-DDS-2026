package ar.edu.utn.frba.ddsi.notificaciones.mappers;

import ar.edu.utn.frba.ddsi.notificaciones.dto.NotificacionDTO;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import org.springframework.stereotype.Component;

@Component
public class NotificacionMapper {
    public NotificacionDTO notificacionDTO(Notificacion notificacion) {
        NotificacionDTO notificacionDTO = new NotificacionDTO();

        notificacionDTO.setAsunto(notificacion.getMensaje().getAsunto());
        notificacionDTO.setCuerpo(notificacion.getMensaje().getCuerpo());
        notificacionDTO.setDireccionDeContacto(notificacion.getDireccionDeContacto());
        notificacionDTO.setEstado(notificacion.getEstado().toString());
        notificacionDTO.setFechaCreacion(notificacion.getFechaCreacion().toString());
        if (notificacion.getFechaEnvio() != null) {
            notificacionDTO.setFechaEnvio(notificacion.getFechaEnvio().toString());
        }

        return notificacionDTO;
    }
}
