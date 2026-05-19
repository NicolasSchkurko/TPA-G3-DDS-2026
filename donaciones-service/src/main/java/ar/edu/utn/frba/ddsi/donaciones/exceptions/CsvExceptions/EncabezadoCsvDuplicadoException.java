package ar.edu.utn.frba.ddsi.donaciones.exceptions.CsvExceptions;

public class EncabezadoCsvDuplicadoException extends RuntimeException {
  public EncabezadoCsvDuplicadoException(String encabezado) {
    super("Se detectó un encabezado duplicado en el archivo CSV: " + encabezado);
  }
}