package ar.edu.utn.frba.ddsi.donaciones.dto.incentivos;

import java.time.YearMonth;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
//Para recibir la actividad de una persona de un mes determinado.
public class ActividadDTO {
  private YearMonth periodo;
  private Integer cantidadDonaciones;
  private Integer organizacionesAyudadas;
}
