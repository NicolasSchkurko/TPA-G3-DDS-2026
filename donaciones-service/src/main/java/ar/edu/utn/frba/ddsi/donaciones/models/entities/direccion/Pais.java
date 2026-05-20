package ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class Pais {
  private String nombre;

  public Pais(String nombre) {
    this.nombre = nombre;
  }

  @Override
  public String toString() {
    return nombre;
  }
}