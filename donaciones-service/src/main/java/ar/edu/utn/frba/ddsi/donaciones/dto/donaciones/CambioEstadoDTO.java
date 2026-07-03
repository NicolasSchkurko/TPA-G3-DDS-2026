package ar.edu.utn.frba.ddsi.donaciones.dto.donaciones;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CambioEstadoDTO {
  private String nuevoEstado;
  private String justificacion;
}