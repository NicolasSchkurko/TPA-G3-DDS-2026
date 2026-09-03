package ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Provincia {

  @Id
  private UUID id = UUID.randomUUID();

  private String nombre;

  @ManyToOne
  @JoinColumn(name = "pais_id")
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

  @Override
  public String toString() {
    return nombre + ", pais=" + pais;
  }
}
