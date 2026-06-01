package ar.edu.utn.frba.ddsi.notificaciones.models.entities.SolicitudNotificacion;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Destinatario.Destinatario;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Mensaje.Mensaje;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

public class SolicitudNotificacion {
    private String servicioOrigen;
    @Getter
    private String codigoEvento;
    @Setter
    @Getter
    private Long idDestinatario;
    @Setter
    @Getter
    private Map<String,Object> parametros;
    private LocalDateTime fecha;

    public SolicitudNotificacion(String servicioOrigen, String codigoEvento, Long idDestinatario, Map<String,Object> parametros) {
        this.servicioOrigen = servicioOrigen;
        this.codigoEvento = codigoEvento;
        this.idDestinatario = idDestinatario;
        this.parametros = parametros;
        this.fecha = LocalDateTime.now();
    }
}
