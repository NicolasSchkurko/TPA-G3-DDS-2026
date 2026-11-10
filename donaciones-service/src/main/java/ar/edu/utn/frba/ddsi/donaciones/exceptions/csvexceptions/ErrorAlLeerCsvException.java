package ar.edu.utn.frba.ddsi.donaciones.exceptions.csvexceptions;

public class ErrorAlLeerCsvException extends RuntimeException {
  public ErrorAlLeerCsvException(String mensaje, Throwable causa) {
    super(mensaje, causa);
  }
}