package ar.edu.utn.frba.ddsi.notificaciones.services;

import ar.edu.utn.frba.ddsi.notificaciones.dto.SolicitudNotificacionDTO;
import ar.edu.utn.frba.ddsi.notificaciones.exceptions.NotificacionExceptions.ErrorAlEnviarNotificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeEnvio.MedioDeEnvio;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeEnvio.MedioDeEnvioFactory;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotificadorService {

    private final MedioDeEnvioFactory factory;

    @Autowired
    public NotificadorService(MedioDeEnvioFactory factory) {
        this.factory = factory;
    }

    public void procesarSolicitudDeNotificacion(SolicitudNotificacionDTO solicitudNotificacionDTO){
        Notificacion notificacion = crearNotificacion(solicitudNotificacionDTO);
        String direccionContacto = solicitudNotificacionDTO.getDireccionDeContacto();
        String tipoDeMedioDeContacto = solicitudNotificacionDTO.getMedioDeContacto();
        enviarNotificacion(tipoDeMedioDeContacto,direccionContacto, notificacion);
    }

    // Crea una Notificacion a partir de una SolicitudNotificacion
    private Notificacion crearNotificacion(SolicitudNotificacionDTO solicitud) {
        String direccionDeContacto = solicitud.getDireccionDeContacto();
        String asunto = solicitud.getAsuntoMensaje();
        String cuerpo = solicitud.getCuerpoMensaje();

        Mensaje mensaje = new Mensaje(asunto, cuerpo);

        return new Notificacion(direccionDeContacto, mensaje);
    }

    // Por ahora solo envia al medio predeterminado
    private void enviarNotificacion(String tipoMedioContacto ,String direccionContacto, Notificacion notificacion) {

        try {
            MedioDeEnvio medioDeContacto = factory.mapearAMedioEnvio(tipoMedioContacto);
            medioDeContacto.enviarNotificacion(direccionContacto, notificacion);
            notificacion.marcarEnviada();

        } catch (IllegalArgumentException ex) {

            notificacion.marcarFallida();
            throw new ErrorAlEnviarNotificacion("Ocurrio un problema inesperado al enviar la notificacion", ex);
        }
    }
}