package ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas;

public class Humano {
  private String nombre;
  private String apellido;
  private int edad;
  private int numeroDeDocumento;
  private Genero genero;

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
}
