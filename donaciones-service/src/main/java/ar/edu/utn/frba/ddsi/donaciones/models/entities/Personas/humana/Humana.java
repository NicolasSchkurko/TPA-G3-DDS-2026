package ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Persona;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Humana extends Persona {
  private String nombre;
  private String apellido;
  private int edad;
  private int numeroDeDocumento;
  Genero genero;

  public Humana(
      String nombre,
      String apellido,
      int edad,
      int numeroDeDocumento,
      Genero genero
  ) {
    super();
    this.nombre = nombre;
    this.apellido = apellido;
    this.edad = edad;
    this.numeroDeDocumento = numeroDeDocumento;
    this.genero = genero;
  }
  @Override
  public String getNombreDeUsuario(){
    return  nombre + " "+ apellido;
  }

}
