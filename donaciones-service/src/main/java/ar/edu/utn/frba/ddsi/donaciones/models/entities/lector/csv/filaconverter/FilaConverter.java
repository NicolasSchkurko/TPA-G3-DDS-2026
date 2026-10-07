package ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.filaconverter;

import java.util.List;
import java.util.Map;

/** Convierte una fila de CSV en un objeto: clave = nombre de la columna, valor = celda. */

public interface FilaConverter<T> {
  T convertir(Map<String, String> row);
}
