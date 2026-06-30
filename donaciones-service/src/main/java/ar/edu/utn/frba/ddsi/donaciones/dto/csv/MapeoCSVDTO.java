package ar.edu.utn.frba.ddsi.donaciones.dto.csv;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MapeoCSVDTO {
  private String campo;
  private List<String> nombresColumnas;
}