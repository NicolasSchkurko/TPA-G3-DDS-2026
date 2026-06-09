package ar.edu.utn.frba.ddsi.notificaciones.models.entities.GestorNotificacion;

import ar.edu.utn.frba.ddsi.notificaciones.exceptions.NotificacionExceptions.DestinatarioInvalidoException;
import ar.edu.utn.frba.ddsi.notificaciones.exceptions.NotificacionExceptions.ErrorAlEnviarNotificacion;
import ar.edu.utn.frba.ddsi.notificaciones.exceptions.NotificacionExceptions.MensajeInvalidoException;
import ar.edu.utn.frba.ddsi.notificaciones.exceptions.NotificacionExceptions.SolicitudInvalidaException;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.TipoNotificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Destinatario.Destinatario;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.SolicitudNotificacion.SolicitudNotificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.repositories.RepositorioDestinatarios;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

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
        Destinatario destinatario = buscarDestinatario(solicitud.getIdDestinatario());
        String asunto = solicitud.getAsuntoMensaje();
        String cuerpo = solicitud.getCuerpoMensaje();

        Mensaje mensaje = new Mensaje(asunto, cuerpo);

        return new Notificacion(destinatario, mensaje);
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

    // Por ahora siempre devuelve el medio predeterminado incluso si no acepta ese tipo de notificacion,
    // para el resto de los medios si los filtra por tipo permitido

    public Destinatario buscarDestinatario(Long idDestinatario) {
        Destinatario destinatario = repositorio.getPersonaPorNumeroId(idDestinatario.shortValue());
        return destinatario;
    }
}
