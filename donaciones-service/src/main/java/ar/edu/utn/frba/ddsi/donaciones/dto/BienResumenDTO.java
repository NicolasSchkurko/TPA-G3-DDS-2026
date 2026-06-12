package ar.edu.utn.frba.ddsi.donaciones.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BienResumenDTO {
  private String descripcion;
  private String subcategoria;
  private String categoria;
  private Integer cantidad;
  private String unidadDeMedida;
  private String tipoBien; // "CON_ESTADO" o "PERECEDERO"
  private Boolean usado; // solo para BienConEstado
  private LocalDate fechaVencimiento; // solo para BienPerecedero
}
