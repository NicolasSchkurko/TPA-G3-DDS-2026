package ar.edu.utn.frba.ddsi.donaciones.models.entities.lector;

import java.io.InputStream;
import java.util.List;

public interface Lector<T> {
  /**
   * Importa datos desde un InputStream y los convierte en una lista de objetos de tipo T.
   * @param contenido
   * @return
   */
  List<T> importar(InputStream contenido);
}
