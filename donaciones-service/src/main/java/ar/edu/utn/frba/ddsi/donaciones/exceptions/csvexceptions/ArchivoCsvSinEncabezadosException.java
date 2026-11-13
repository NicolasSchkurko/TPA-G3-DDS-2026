package ar.edu.utn.frba.ddsi.donaciones.exceptions.csvexceptions;

public class ArchivoCsvSinEncabezadosException extends RuntimeException {
  public ArchivoCsvSinEncabezadosException(String mensaje) {
    super(mensaje);
  }
}