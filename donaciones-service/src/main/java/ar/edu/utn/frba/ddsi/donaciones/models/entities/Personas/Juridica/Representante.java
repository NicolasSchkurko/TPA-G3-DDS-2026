package ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Humana;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter

public class Representante {
  private Humana humana;
  private boolean activo;

  public Representante(Humana humana, boolean activo) {

    this.humana = humana;
    this.activo = activo;
  }

}
