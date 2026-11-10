package ar.edu.utn.frba.ddsi.notificaciones.dto.n8n;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Mensaje.Mensaje;
import lombok.Getter;


public class NotificacionPayload {
    @Getter
    private String canal;
    @Getter
    private String direccionContacto;
    @Getter
    private Mensaje mensaje;

    /** Link estructurado a un mapa/adjunto; n8n lo recibe aparte del texto. */
    @Getter
    private String urlMapa;

    public NotificacionPayload(String canal, String direccionContacto, Mensaje mensaje) {
        this(canal, direccionContacto, mensaje, mensaje != null ? mensaje.getUrlMapa() : null);
    }

    public NotificacionPayload(String canal, String direccionContacto, Mensaje mensaje,
                               String urlMapa) {
        this.canal = canal;
        this.direccionContacto = direccionContacto;
        this.mensaje = mensaje;
        this.urlMapa = urlMapa;
    }
}
