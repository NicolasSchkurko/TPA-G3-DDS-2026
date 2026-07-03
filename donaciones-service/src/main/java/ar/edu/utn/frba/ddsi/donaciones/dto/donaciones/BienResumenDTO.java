package ar.edu.utn.frba.ddsi.donaciones.dto.donaciones;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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
