package ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv;

import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa la relación entre un campo de destino (representado por un String)
 * y una lista de posibles nombres de columna en un archivo CSV.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class MapeoCSV {

  private String campo;

  private List<String> nombresColumnas = new ArrayList<>();
}