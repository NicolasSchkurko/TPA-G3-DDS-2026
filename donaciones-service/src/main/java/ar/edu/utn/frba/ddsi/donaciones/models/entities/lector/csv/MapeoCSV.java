package ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv;

import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa la relación entre un campo de destino (representado por un String)
 * y una lista de posibles nombres de columna en un archivo CSV.
 * Esto normaliza la estructura de la base de datos de forma genérica.
 */
//@Entity
public class MapeoCSV {

//  @Id
//  @GeneratedValue
//  private Long id;

  // Se cambia a String para hacerlo genérico y no depender de un Enum.
//  @Column(name = "campo_destino")
  private String campo;

  //  @ElementCollection(fetch = FetchType.EAGER)
//  @CollectionTable(name = "mapeo_csv_nombres_columna", joinColumns = @JoinColumn(name = "mapeo_id"))
//  @Column(name = "nombre_columna")
  private List<String> nombresColumnas = new ArrayList<>();

  // Constructor para JPA
  protected MapeoCSV() {
  }

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

