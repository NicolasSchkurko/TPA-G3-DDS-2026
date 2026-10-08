package ar.edu.utn.frba.ddsi.notificaciones.services;

import ar.edu.utn.frba.ddsi.notificaciones.dto.SolicitudNotificacionDTO;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.gestores.GestorNotificaciones;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class NotificadorService {

    private final GestorNotificaciones gestorNotificaciones;

    public NotificadorService(GestorNotificaciones gestorNotificaciones) {
        this.gestorNotificaciones = gestorNotificaciones;
    }

    public void procesarSolicitudDeNotificacion(SolicitudNotificacionDTO solicitudNotificacionDTO) {
        gestorNotificaciones.enviarSolicitudDeNotificacion(
                solicitudNotificacionDTO.getMedioDeContacto(),
                solicitudNotificacionDTO.getDireccionDeContacto(),
                solicitudNotificacionDTO.getAsuntoMensaje(),
                solicitudNotificacionDTO.getCuerpoMensaje());
    }

    public Optional<Notificacion> obtenerPorId(UUID id) {
        return gestorNotificaciones.obtenerNotificacionPorId(id);
    }
}
