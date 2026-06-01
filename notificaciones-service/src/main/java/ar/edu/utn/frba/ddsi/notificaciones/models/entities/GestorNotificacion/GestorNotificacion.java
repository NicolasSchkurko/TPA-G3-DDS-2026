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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GestorNotificacion {

    public Notificacion procesarSolicitud(SolicitudNotificacion solicitud) {
        Notificacion notificacion = crearNotificacion(solicitud);
        enviarNotificacion(notificacion);

        return notificacion;
    }

    public void validarSolicitud(SolicitudNotificacion solicitud) {
        if (solicitud == null)
            throw new SolicitudInvalidaException("La solicitud de notificación no puede ser nula");

        if (solicitud.getIdDestinatario() == null)
            throw new DestinatarioInvalidoException("El destinatario de la notificación no puede ser nulo");

        if (solicitud.getParametros() == null)
            throw new MensajeInvalidoException("El mensaje a enviar no debe ser nulo");
    }

    // Crea una Notificacion a partir de una SolicitudNotificacion
    private Notificacion crearNotificacion(SolicitudNotificacion solicitud) {
        Destinatario destinatario = buscarDestinatario(solicitud.getIdDestinatario());
        TipoNotificacion tipoNotificacion = mapearCodigoATipoNotificacion(solicitud.getCodigoEvento());
        Mensaje mensaje = generarMensaje(tipoNotificacion, solicitud.getParametros());

        return new Notificacion(tipoNotificacion, destinatario, mensaje);
    }

    // Por ahora solo envia al medio predeterminado
    public void enviarNotificacion(Notificacion notificacion) {
        Destinatario destinatario = notificacion.getDestinatario();
        MedioDeContacto medioPredeterminado = destinatario.getMedioDeContactoPredeterminado();

        try {

            medioPredeterminado.enviarNotificacion(notificacion);
            notificacion.marcarEnviada();

        } catch (IllegalArgumentException ex) {

            notificacion.marcarFallida();
            throw new ErrorAlEnviarNotificacion("Ocurrio un problema inesperado al enviar la notificacion", ex);
        }
    }

    // Por ahora siempre devuelve el medio predeterminado incluso si no acepta ese tipo de notificacion,
    // para el resto de los medios si los filtra por tipo permitido
    public List<MedioDeContacto> obtenerMediosCompatibles(Destinatario destinatario, TipoNotificacion tipo) {
        List<MedioDeContacto> listaMediosCompatibles = new ArrayList<>();
        List<MedioDeContacto> listaMedios = destinatario.getMediosDeContacto().getListaMediosDeContacto();

        listaMedios.forEach(medio -> {
            if(medio.permiteTipoNotificacion(tipo))  listaMediosCompatibles.add(medio);
        });

        // agrega el medio predeterminado si ya no estaba
        MedioDeContacto medioPredeterminado = destinatario.getMedioDeContactoPredeterminado();
        if(!listaMediosCompatibles.contains(medioPredeterminado)) listaMediosCompatibles.add(medioPredeterminado);

        return listaMediosCompatibles;
    }

    public Destinatario buscarDestinatario(Long idDestinatario) {
        Destinatario destinatario = null;
        return destinatario;
    }

    // Tendra una lista de mensajes predeterminados de donde se eligira en base a TipoNotificacion
    // y se completara con los parametros.
    public Mensaje generarMensaje(TipoNotificacion tipoNotificacion, Map<String,Object> parametros) {
        Mensaje mensaje = null;
        return mensaje;
    };

    // Seguro mapeara con una lista de codigos a tipoNotificacion
    public TipoNotificacion mapearCodigoATipoNotificacion(String codigoEvento){
        TipoNotificacion tipoNotificacion = null;
        return tipoNotificacion;
    };
}
