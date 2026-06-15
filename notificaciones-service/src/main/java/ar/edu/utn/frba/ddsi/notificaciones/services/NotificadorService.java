package ar.edu.utn.frba.ddsi.notificaciones.services;

import ar.edu.utn.frba.ddsi.notificaciones.dto.SolicitudNotificacionDTO;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.GestorNotificacion.GestorNotificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeEnvio.MedioDeEnvioFactory;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.SolicitudNotificacion.SolicitudNotificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.repositories.RepositorioNotificaciones;
import org.springframework.stereotype.Service;

@Service
public class NotificadorService {
    private final GestorNotificacion gestor;

    public NotificadorService(MedioDeEnvioFactory medioDeEnvioFactory, RepositorioNotificaciones repositorioNotificaciones) {
        this.gestor = new GestorNotificacion(medioDeEnvioFactory, repositorioNotificaciones);
    }

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
