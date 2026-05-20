package ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.Scanner;

@Getter
@Setter
public class Humano {
  private String nombre;
  private String apellido;
  private int edad;
  private int numeroDeDocumento;
  Genero genero;

  public Humano(
      String nombre,
      String apellido,
      int edad,
      int numeroDeDocumento,
      Genero genero
  ) {
    this.nombre = nombre;
    this.apellido = apellido;
    this.edad = edad;
    this.numeroDeDocumento = numeroDeDocumento;
    this.genero = genero;
  }

  @Override
  public String toString() {
    return "Humano{nombre=" + nombre + ", apellido=" + apellido + ", edad=" + edad + ", numero de documento=" + numeroDeDocumento + ", genero=" + genero + '}';
  }
}
