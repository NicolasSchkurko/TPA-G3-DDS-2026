package ar.edu.utn.frba.ddsi.notificaciones.services;
import java.util.Objects;
import java.util.stream.Collectors;

import ar.edu.utn.frba.ddsi.notificaciones.dto.SolicitudNotificacionDTO;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.GestorNotificacion.GestorNotificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.SolicitudNotificacion.SolicitudNotificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.repositories.RepositorioDestinatarios;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class NotificadorService {

    private final GestorNotificacion gestor = GestorNotificacion.getInstance();
    public void procesarSolicitudDeNotificacion(SolicitudNotificacionDTO solicitudNotificacionDTO){
        SolicitudNotificacion solicitud = new SolicitudNotificacion(
                solicitudNotificacionDTO.getMedioDeContacto(),
                solicitudNotificacionDTO.getDireccionDeContacto(),
                solicitudNotificacionDTO.getCuerpoMensaje(),
                solicitudNotificacionDTO.getAsuntoMensaje()
        );
        gestor.procesarSolicitud(solicitud);

    }
}