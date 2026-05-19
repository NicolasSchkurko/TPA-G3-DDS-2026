package ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv;

import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa la relación entre un campo de destino (representado por un String)
 * y una lista de posibles nombres de columna en un archivo CSV.
 */
public class MapeoCSV {

  private Long id;

  private String campo;

  private List<String> nombresColumnas = new ArrayList<>();

  public MapeoCSV(String campo, List<String> nombresColumnas) {
    this.campo = campo;
    this.nombresColumnas = nombresColumnas;
  }
  public String getCampo() {
    return campo;
  }
  public List<String> getNombresColumnas() {
    return nombresColumnas;
  }
}