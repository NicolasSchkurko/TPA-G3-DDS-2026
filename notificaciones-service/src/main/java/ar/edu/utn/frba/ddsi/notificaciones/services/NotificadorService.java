package ar.edu.utn.frba.ddsi.notificaciones.services;

import ar.edu.utn.frba.ddsi.notificaciones.dto.SolicitudNotificacionDTO;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeEnvio.MedioDeEnvioFactory;
import ar.edu.utn.frba.ddsi.notificaciones.models.gestores.GestorNotificaciones;
import ar.edu.utn.frba.ddsi.notificaciones.models.repositories.RepositorioNotificaciones;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotificadorService {

    private final GestorNotificaciones gestorNotificaciones;

    @Autowired
    public NotificadorService(GestorNotificaciones gestorNotificaciones) {
        this.gestorNotificaciones = gestorNotificaciones;
    }

    public void procesarSolicitudDeNotificacion(SolicitudNotificacionDTO solicitudNotificacionDTO){
        String direccionContacto = solicitudNotificacionDTO.getDireccionDeContacto();
        String tipoDeMedioDeContacto = solicitudNotificacionDTO.getMedioDeContacto();
        String asunto = solicitudNotificacionDTO.getAsuntoMensaje();
        String cuerpo = solicitudNotificacionDTO.getCuerpoMensaje();

        gestorNotificaciones.enviarSolicitudDeNotificacion(
                tipoDeMedioDeContacto,
                direccionContacto,
                asunto,
                cuerpo);
    }
}