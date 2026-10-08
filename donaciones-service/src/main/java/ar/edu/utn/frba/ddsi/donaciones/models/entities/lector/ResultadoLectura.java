package ar.edu.utn.frba.ddsi.donaciones.models.entities.lector;

import java.util.List;

public class ResultadoLectura<T> {
  private final List<T> elementos;
  private final List<String> errores;
  private final int totalFilas;

  public ResultadoLectura(List<T> elementos, List<String> errores, int totalFilas) {
    this.elementos = elementos;
    this.errores = errores;
    this.totalFilas = totalFilas;
  }

  public List<T> getElementos() {
    return elementos;
  }

  public List<String> getErrores() {
    return errores;
  }

  public int getTotalFilas() {
    return totalFilas;
  }
}
