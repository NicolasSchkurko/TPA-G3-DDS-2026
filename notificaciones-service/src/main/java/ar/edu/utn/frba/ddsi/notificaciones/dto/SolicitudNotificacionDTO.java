package ar.edu.utn.frba.ddsi.notificaciones.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
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
}
