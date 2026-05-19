package ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class Provincia {
  private String nombre;
  private Pais pais;

  public Provincia(
      String nombre,
      Pais pais
  ) {
    this.nombre = nombre;
    this.pais = pais;
  }

  public String getDireccion() {
    return String.format("%s, %s",
        nombre,
        pais.getNombre()
    );
  }

  @Override
  public String toString() {
    return nombre + ", pais=" + pais;
  }
}
