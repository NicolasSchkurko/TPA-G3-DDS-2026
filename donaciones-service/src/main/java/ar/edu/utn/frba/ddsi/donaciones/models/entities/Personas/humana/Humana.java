package ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Persona;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Humana extends Persona {
  private String nombre;
  private String apellido;
  private int edad;
  private int numeroDeDocumento;

  @Enumerated(EnumType.STRING)
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
