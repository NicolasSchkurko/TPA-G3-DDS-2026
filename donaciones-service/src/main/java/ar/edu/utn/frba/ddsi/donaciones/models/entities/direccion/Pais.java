package ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pais {

  @Id
  private UUID id = UUID.randomUUID();

  private String nombre;

  public Pais(String nombre) {
    this.nombre = nombre;
  }

  @Override
  public String toString() {
    return nombre;
  }
}