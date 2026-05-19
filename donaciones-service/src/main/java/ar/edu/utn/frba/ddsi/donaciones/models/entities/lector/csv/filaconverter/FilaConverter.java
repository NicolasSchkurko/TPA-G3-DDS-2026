package ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.filaconverter;

import java.util.List;
import java.util.Map;

/**
 * Interfaz funcional para convertir una fila de un CSV (representada como un Map)
 * en un objeto de un tipo específico T.
 *
 * @param <T> El tipo de objeto a crear.
 */

public interface FilaConverter<T> {
  /**
   * Convierte una fila de CSV en un objeto.
   *
   * @param row Un mapa donde la clave es el nombre de la columna y el valor es el dato de la celda.
   * @return Un objeto de tipo T, o null si la fila no se puede convertir.
   */
  T convertir(Map<String, String> row);

}
