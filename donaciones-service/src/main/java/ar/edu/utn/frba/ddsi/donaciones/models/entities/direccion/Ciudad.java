package ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class Ciudad {
  private String nombre;
  private Provincia provincia;

  public Ciudad(
      String nombre,
      Provincia provincia
  ) {
    this.nombre = nombre;
    this.provincia = provincia;
  }

  public String getDireccion() {
    return String.format("%s, %s",
        nombre,
        provincia.getDireccion()
    );
  }

}
