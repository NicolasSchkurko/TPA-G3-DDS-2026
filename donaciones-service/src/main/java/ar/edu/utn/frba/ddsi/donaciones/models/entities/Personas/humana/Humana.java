package ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Persona;
import jakarta.persistence.Column;
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
  @Column(length = 250)
  private String nombre;

  @Column(length = 250)
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
    if ((nombre == null || nombre.isBlank()) && (apellido == null || apellido.isBlank())) {
      throw new IllegalArgumentException("La persona humana requiere al menos un nombre o apellido");
    }
    this.nombre = nombre != null ? nombre.trim() : null;
    this.apellido = apellido != null ? apellido.trim() : null;
    this.edad = edad;
    this.numeroDeDocumento = numeroDeDocumento;
    this.genero = genero;
  }

  @Override
  public String getNombreDeUsuario() {
    if ((nombre == null || nombre.isBlank()) && (apellido == null || apellido.isBlank())) {
      throw new IllegalStateException("El donante no tiene nombre ni apellido");
    }
    if (nombre == null || nombre.isBlank()) {
      return apellido.trim();
    }
    if (apellido == null || apellido.isBlank()) {
      return nombre.trim();
    }
    return (nombre.trim() + " " + apellido.trim()).trim();
  }
}
