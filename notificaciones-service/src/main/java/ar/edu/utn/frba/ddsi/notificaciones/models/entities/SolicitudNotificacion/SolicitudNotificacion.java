package ar.edu.utn.frba.ddsi.notificaciones.models.entities.SolicitudNotificacion;

import ar.edu.utn.frba.ddsi.notificaciones.exceptions.NotificacionExceptions.DireccionInvalidoException;
import ar.edu.utn.frba.ddsi.notificaciones.exceptions.NotificacionExceptions.MensajeInvalidoException;
import ar.edu.utn.frba.ddsi.notificaciones.exceptions.NotificacionExceptions.TipoMedioDeContactoInvalidoException;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

public class SolicitudNotificacion {
    @Setter
    @Getter
    private String tipoMedioDeContacto;
    @Getter
    private String direccionDeContacto;
    private LocalDateTime fecha;
    @Getter
    private String cuerpoMensaje;
    @Getter
    private String asuntoMensaje;

    public SolicitudNotificacion(String tipoMedioDeContacto, String direccionDeContacto, String cuerpoMensaje, String asuntoMensaje) {
        this.tipoMedioDeContacto = tipoMedioDeContacto;
        this.direccionDeContacto = direccionDeContacto;
        this.cuerpoMensaje = cuerpoMensaje;
        this.asuntoMensaje = asuntoMensaje;
        this.fecha = LocalDateTime.now();

        if (this.direccionDeContacto == null)
            throw new DireccionInvalidoException("La direccion de contacto de la notificación no puede ser nulo");
        if (this.cuerpoMensaje == null)
            throw new MensajeInvalidoException("El cuerpo del mensaje a enviar no debe ser nulo");
        if (this.asuntoMensaje == null)
            throw new MensajeInvalidoException("El asunto del mensaje a enviar no debe ser nulo");
        if (this.tipoMedioDeContacto == null)
            throw new TipoMedioDeContactoInvalidoException("El tipo de contacto a enviar no debe ser nulo");
    }

}
