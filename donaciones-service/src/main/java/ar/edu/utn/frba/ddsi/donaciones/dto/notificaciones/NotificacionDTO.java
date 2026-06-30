package ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones;

import java.util.Locale;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class NotificacionDTO {
  private String medioDeContacto;
  private String direccionDeContacto;
  private String cuerpoMensaje;
  private String asuntoMensaje;

  public NotificacionDTO(
      String medioDeContacto,
      String direccionDeContacto,
      String cuerpoMensaje,
      String asuntoMensaje
  ) {
    this.medioDeContacto = normalizarTipo(medioDeContacto);
    this.direccionDeContacto = direccionDeContacto;
    this.cuerpoMensaje = cuerpoMensaje;
    this.asuntoMensaje = asuntoMensaje;
  }

  public NotificacionDTO(
      MediosContactoDTO medioDeContacto,
      String direccionDeContacto,
      String cuerpoMensaje,
      String asuntoMensaje
  ) {
    this(
        medioDeContacto != null ? medioDeContacto.getTipo() : null,
        direccionDeContacto,
        cuerpoMensaje,
        asuntoMensaje
    );
  }

  private static String normalizarTipo(String tipo) {
    if (tipo == null || tipo.isBlank()) {
      return tipo;
    }
    if ("MAIL".equalsIgnoreCase(tipo) || "EMAIL".equalsIgnoreCase(tipo)) {
      return "email";
    }
    return tipo.toLowerCase(Locale.ROOT);
  }
}
