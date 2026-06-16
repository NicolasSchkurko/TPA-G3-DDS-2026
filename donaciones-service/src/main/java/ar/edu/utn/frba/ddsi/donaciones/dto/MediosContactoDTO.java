package ar.edu.utn.frba.ddsi.donaciones.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class MediosContactoDTO {
    private String tipo; // "EMAIL", "TELEFONO", "WHATSAPP", etc.
    private String valor; // El correo electrónico o número telefónico

  @Getter
  @Setter

  public static class NotificacionDTO {
    private String medioDeContacto;
    private String direccionContacto;
    private String cuerpoMensaje;
    private String asuntoMensaje;

    public NotificacionDTO(String medioDeContacto,
                                 String direccionContacto,
                                 String cuerpoMensaje,
                                 String asuntoMensaje){
      this.medioDeContacto = medioDeContacto;
      this.direccionContacto = direccionContacto;
      this.cuerpoMensaje = cuerpoMensaje;
      this.asuntoMensaje = asuntoMensaje;
    }

  }
}

