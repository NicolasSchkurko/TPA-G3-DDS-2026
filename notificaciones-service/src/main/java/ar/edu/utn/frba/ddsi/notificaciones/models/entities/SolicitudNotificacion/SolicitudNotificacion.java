package ar.edu.utn.frba.ddsi.notificaciones.models.entities.SolicitudNotificacion;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Destinatario.Destinatario;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Mensaje.Mensaje;

import java.time.LocalDateTime;

public class SolicitudNotificacion {
    private Destinatario destinatario;
    private Mensaje mensaje;
    private LocalDateTime fecha;

    public SolicitudNotificacion(Destinatario destinatario, Mensaje mensaje) {
        this.destinatario = destinatario;
        this.mensaje = mensaje;
        this.fecha = LocalDateTime.now();
    }
}
