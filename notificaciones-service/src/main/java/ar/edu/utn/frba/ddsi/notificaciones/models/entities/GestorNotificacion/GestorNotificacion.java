package ar.edu.utn.frba.ddsi.notificaciones.models.entities.GestorNotificacion;

import ar.edu.utn.frba.ddsi.notificaciones.exceptions.NotificacionExceptions.ErrorAlEnviarNotificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeEnvio.MedioDeEnvio;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.SolicitudNotificacion.SolicitudNotificacion;

public class GestorNotificacion {
    private static  GestorNotificacion instanciaUnica;

    private GestorNotificacion() {}

    public static GestorNotificacion getInstance() {
        if (instanciaUnica == null) {
            instanciaUnica = new GestorNotificacion();
        }
        return instanciaUnica;
    }

    public Notificacion procesarSolicitud(SolicitudNotificacion solicitud) {
        Notificacion notificacion = crearNotificacion(solicitud);
        String direccionContacto = solicitud.getDireccionDeContacto();

        enviarNotificacion(direccionContacto, notificacion);

        return notificacion;
    }

    // Crea una Notificacion a partir de una SolicitudNotificacion
    private Notificacion crearNotificacion(SolicitudNotificacion solicitud) {
        String direccionDeContacto = solicitud.getDireccionDeContacto();
        String asunto = solicitud.getAsuntoMensaje();
        String cuerpo = solicitud.getCuerpoMensaje();

        Mensaje mensaje = new Mensaje(asunto, cuerpo);

        return new Notificacion(direccionDeContacto, mensaje);
    }
    
    private MedioDeEnvio mapearStringAMedioDeEnvio(String tipoMedioContacto){
        return null;
    }

    // Por ahora solo envia al medio predeterminado
    public void enviarNotificacion(String direccionContacto, Notificacion notificacion) {

        try {
            MedioDeEnvio medioDeContacto = mapearStringAMedioDeEnvio(direccionContacto);
            medioDeContacto.enviarNotificacion(direccionContacto, notificacion);
            notificacion.marcarEnviada();

        } catch (IllegalArgumentException ex) {

            notificacion.marcarFallida();
            throw new ErrorAlEnviarNotificacion("Ocurrio un problema inesperado al enviar la notificacion", ex);
        }
    }
}
