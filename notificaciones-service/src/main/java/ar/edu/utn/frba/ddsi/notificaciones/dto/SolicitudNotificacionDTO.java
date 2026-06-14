package ar.edu.utn.frba.ddsi.notificaciones.dto;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
public class SolicitudNotificacionDTO {
    private String medioDeContacto;
    private String direccionDeContacto;
    private String cuerpoMensaje;
    private String asuntoMensaje;
}
