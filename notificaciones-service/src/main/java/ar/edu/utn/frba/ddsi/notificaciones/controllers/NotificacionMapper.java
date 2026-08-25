package ar.edu.utn.frba.ddsi.notificaciones.controllers;

import ar.edu.utn.frba.ddsi.notificaciones.dto.NotificacionDTO;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;

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
