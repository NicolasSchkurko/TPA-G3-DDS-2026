package ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion;


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

}
