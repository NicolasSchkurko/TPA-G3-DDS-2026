package ar.edu.utn.frba.ddsi.notificaciones.dto;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
public class SolicitudNotificacionDTO {
    private String servicioOrigen;
    private Long idDestinatario;
    private LocalDateTime fecha;
    private String cuerpoMensaje;
    private String asuntoMensaje;
}
