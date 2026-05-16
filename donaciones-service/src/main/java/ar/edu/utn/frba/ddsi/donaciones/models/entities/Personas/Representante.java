package ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter

public class Representante {
  private Humano humano;
  private boolean activo;

  public Representante(Humano humano, boolean activo) {

    this.humano = humano;
    this.activo = activo;
  }

}
