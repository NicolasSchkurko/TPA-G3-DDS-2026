package ar.edu.utn.frba.ddsi.notificaciones.models.gestores;

import ar.edu.utn.frba.ddsi.notificaciones.dto.Notificaciones.SolicitudNotificacionDTO;
import ar.edu.utn.frba.ddsi.notificaciones.exceptions.NotificacionNoReintentableException;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.EstadoNotificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** Reglas de dominio de una solicitud de notificación; no persiste ni publica. */
@Component
public class GestorSolicitudes {

    public void validar(SolicitudNotificacionDTO solicitud) {
        if (solicitud == null) {
            throw new IllegalArgumentException("La solicitud no puede ser nula");
        }
        if (solicitud.getMedioDeContacto() == null || solicitud.getMedioDeContacto().isBlank()) {
            throw new IllegalArgumentException("Solicitud sin medioDeContacto obligatorio");
        }
        if (solicitud.getDireccionDeContacto() == null || solicitud.getDireccionDeContacto().isBlank()) {
            throw new IllegalArgumentException("Solicitud sin direccionDeContacto obligatoria");
        }
        if (solicitud.getAsuntoMensaje() == null || solicitud.getAsuntoMensaje().isBlank()) {
            throw new IllegalArgumentException("Solicitud sin asuntoMensaje obligatorio");
        }
        if (solicitud.getCuerpoMensaje() == null || solicitud.getCuerpoMensaje().isBlank()) {
            throw new IllegalArgumentException("Solicitud sin cuerpoMensaje obligatorio");
        }
        if (solicitud.getUrlMapa() != null && solicitud.getUrlMapa().length() > 1024) {
            throw new IllegalArgumentException("Solicitud con urlMapa de más de 1024 caracteres");
        }
    }

    /** Si ya existe una notificación para el idSolicitud, devuelve su id para no duplicar. */
    public Optional<UUID> idSiYaExiste(Optional<Notificacion> existente) {
        return existente.map(Notificacion::getId);
    }

    /** Una notificación ya enviada o en curso no se puede republicar. */
    public void validarReintento(Notificacion notificacion) {
        if (notificacion.getEstado() == EstadoNotificacion.ENVIADA
                || notificacion.getEstado() == EstadoNotificacion.ENVIANDO) {
            throw new NotificacionNoReintentableException(notificacion.getId());
        }
    }
}
