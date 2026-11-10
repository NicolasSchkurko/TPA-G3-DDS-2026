package ar.edu.utn.frba.ddsi.notificaciones.dto.Notificaciones;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SolicitudNotificacionDTO {

    private String idSolicitud;

    @NotBlank(message = "es obligatorio")
    private String medioDeContacto;

    @NotBlank(message = "es obligatorio")
    private String direccionDeContacto;

    @NotBlank(message = "es obligatorio")
    private String asuntoMensaje;

    @NotBlank(message = "es obligatorio")
    private String cuerpoMensaje;

    /** Opcional: link estructurado a un mapa o adjunto (por ejemplo, el seguimiento de una ruta). */
    @Size(max = 1024, message = "no puede superar 1024 caracteres")
    private String urlMapa;
}
