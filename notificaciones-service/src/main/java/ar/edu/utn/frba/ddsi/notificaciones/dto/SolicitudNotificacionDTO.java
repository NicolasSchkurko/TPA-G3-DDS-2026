package ar.edu.utn.frba.ddsi.notificaciones.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SolicitudNotificacionDTO {

    @NotBlank(message = "es obligatorio")
    private String medioDeContacto;

    @NotBlank(message = "es obligatorio")
    private String direccionDeContacto;

    @NotBlank(message = "es obligatorio")
    private String asuntoMensaje;

    @NotBlank(message = "es obligatorio")
    private String cuerpoMensaje;
}
