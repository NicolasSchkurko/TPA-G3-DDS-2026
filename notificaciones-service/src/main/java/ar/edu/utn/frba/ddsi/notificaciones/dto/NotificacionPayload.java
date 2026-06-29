package ar.edu.utn.frba.ddsi.notificaciones.dto;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Mensaje.Mensaje;
import lombok.Getter;


public class NotificacionPayload {
    @Getter
    private String canal;
    @Getter
    private String direccionContacto;
    @Getter
    private Mensaje mensaje;

    public NotificacionPayload(String canal, String direccionContacto, Mensaje mensaje) {
        this.canal = canal;
        this.direccionContacto = direccionContacto;
        this.mensaje = mensaje;
    }
}
