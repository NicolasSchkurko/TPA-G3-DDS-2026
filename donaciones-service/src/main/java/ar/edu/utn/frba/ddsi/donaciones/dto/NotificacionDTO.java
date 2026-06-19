package ar.edu.utn.frba.ddsi.donaciones.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class NotificacionDTO {
  private MediosContactoDTO medioDeContacto;
  private String direccionContacto;
  private String cuerpoMensaje;
  private String asuntoMensaje;

  public NotificacionDTO(
      MediosContactoDTO medioDeContacto,
      String direccionContacto,
      String cuerpoMensaje,
      String asuntoMensaje
  ) {
    this.medioDeContacto = medioDeContacto;
    this.direccionContacto = direccionContacto;
    this.cuerpoMensaje = cuerpoMensaje;
    this.asuntoMensaje = asuntoMensaje;
  }
}