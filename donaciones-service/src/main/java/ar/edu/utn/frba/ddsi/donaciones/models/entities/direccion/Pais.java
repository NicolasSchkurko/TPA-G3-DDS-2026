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

  public String getNombre() {
    return nombre;
  }
}
