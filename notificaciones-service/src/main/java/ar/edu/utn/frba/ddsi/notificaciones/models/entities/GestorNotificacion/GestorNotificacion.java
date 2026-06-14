package ar.edu.utn.frba.ddsi.notificaciones.models.entities.GestorNotificacion;

import ar.edu.utn.frba.ddsi.notificaciones.exceptions.NotificacionExceptions.ErrorAlEnviarNotificacion;
import ar.edu.utn.frba.ddsi.notificaciones.exceptions.NotificacionExceptions.TipoMedioDeContactoInvalidoException;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Destinatario.Destinatario;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.SolicitudNotificacion.SolicitudNotificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.repositories.RepositorioDestinatarios;

public class GestorNotificacion {
    private static  GestorNotificacion instanciaUnica;
    private RepositorioDestinatarios repositorio;

    private GestorNotificacion() {
        this.repositorio = RepositorioDestinatarios.getInstance();
    }

    public static GestorNotificacion getInstance() {
        if (instanciaUnica == null) {
            instanciaUnica = new GestorNotificacion();
        }
        return instanciaUnica;
    }

    public Notificacion procesarSolicitud(SolicitudNotificacion solicitud) {
        Notificacion notificacion = crearNotificacion(solicitud);
        enviarNotificacion(notificacion);

        return notificacion;
    }

    // Crea una Notificacion a partir de una SolicitudNotificacion
    private Notificacion crearNotificacion(SolicitudNotificacion solicitud) {
        TipoMedioDeContacto medioDeContacto = mapearStringAMedioContacto(solicitud.getTipoMedioDeContacto());
        String direccionDeContacto = solicitud.getDireccionDeContacto();
        String asunto = solicitud.getAsuntoMensaje();
        String cuerpo = solicitud.getCuerpoMensaje();

        Mensaje mensaje = new Mensaje(asunto, cuerpo);

        return new Notificacion(direccionDeContacto, mensaje);
    }
    
    private TipoMedioDeContacto mapearStringAMedioContacto(String tipoMedioContacto){

        return;
    }
    // Por ahora solo envia al medio predeterminado
    public void enviarNotificacion(Notificacion notificacion) {
        Destinatario destinatario = notificacion.getDestinatario();

        try {

            destinatario.getMediosDeContacto().enviarNotificacionAMedios(notificacion);
            notificacion.marcarEnviada();

        } catch (IllegalArgumentException ex) {

            notificacion.marcarFallida();
            throw new ErrorAlEnviarNotificacion("Ocurrio un problema inesperado al enviar la notificacion", ex);
        }
    }
}
