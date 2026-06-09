package ar.edu.utn.frba.ddsi.notificaciones.models.entities.SolicitudNotificacion;

import ar.edu.utn.frba.ddsi.notificaciones.exceptions.NotificacionExceptions.DestinatarioInvalidoException;
import ar.edu.utn.frba.ddsi.notificaciones.exceptions.NotificacionExceptions.MensajeInvalidoException;
import ar.edu.utn.frba.ddsi.notificaciones.exceptions.NotificacionExceptions.ServicioOrigenInvalidoException;
import ar.edu.utn.frba.ddsi.notificaciones.exceptions.NotificacionExceptions.SolicitudInvalidaException;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Destinatario.Destinatario;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Mensaje.Mensaje;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

public class SolicitudNotificacion {
    private String servicioOrigen;
    @Setter
    @Getter
    private Long idDestinatario;
    private LocalDateTime fecha;
    @Getter
    private String cuerpoMensaje;
    @Getter
    private String asuntoMensaje;

    public SolicitudNotificacion(String servicioOrigen, Long idDestinatario, String cuerpoMensaje, String asuntoMensaje) {
        this.servicioOrigen = servicioOrigen;
        this.cuerpoMensaje = cuerpoMensaje;
        this.asuntoMensaje = asuntoMensaje;
        this.idDestinatario = idDestinatario;
        this.fecha = LocalDateTime.now();

        if (this.idDestinatario == null)
            throw new DestinatarioInvalidoException("El destinatario de la notificación no puede ser nulo");
        if (this.cuerpoMensaje == null)
            throw new MensajeInvalidoException("El cuerpo del mensaje a enviar no debe ser nulo");
        if (this.asuntoMensaje == null)
            throw new MensajeInvalidoException("El asunto del mensaje a enviar no debe ser nulo");
        if (this.servicioOrigen == null)
            throw new ServicioOrigenInvalidoException("El servicio de origen a enviar no debe ser nulo");
    }

}
