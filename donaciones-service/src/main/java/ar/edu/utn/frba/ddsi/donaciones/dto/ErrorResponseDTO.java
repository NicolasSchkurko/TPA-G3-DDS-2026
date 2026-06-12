package ar.edu.utn.frba.ddsi.donaciones.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ErrorResponseDTO {
  private String mensaje;
  private int codigoEstado;
}