package ar.edu.utn.frba.ddsi.donaciones.dto;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Estado;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CambioEstadoDTO {
  private Estado nuevoEstado;
  private String justificacion;
}