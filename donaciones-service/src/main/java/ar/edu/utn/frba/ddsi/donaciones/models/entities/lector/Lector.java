package ar.edu.utn.frba.ddsi.donaciones.models.entities.lector;

import java.io.InputStream;

public interface Lector<T> {
  /**
   * Importa datos desde un InputStream y los convierte en una lista de objetos de tipo T,
   * reportando también las filas que no se pudieron convertir (antes se descartaban en
   * silencio, ver LectorCSV.procesarYGuardarFila).
   * @param contenido
   * @return
   */
  ResultadoLectura<T> importar(InputStream contenido);
}
