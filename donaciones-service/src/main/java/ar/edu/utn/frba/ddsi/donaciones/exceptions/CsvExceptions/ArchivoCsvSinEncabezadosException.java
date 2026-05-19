package ar.edu.utn.frba.ddsi.donaciones.exceptions.CsvExceptions;

public class ArchivoCsvSinEncabezadosException extends RuntimeException {
  public ArchivoCsvSinEncabezadosException(String mensaje) {
    super(mensaje);
  }
}