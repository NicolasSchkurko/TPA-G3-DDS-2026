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
public class Ciudad {

  @Id
  private UUID id = UUID.randomUUID();

  private String nombre;

  @ManyToOne
  @JoinColumn(name = "provincia_id")
  private Provincia provincia;

  public Ciudad(
      String nombre,
      Provincia provincia
  ) {
    this.nombre = nombre;
    this.provincia = provincia;
  }

  public String getDireccion() {
    return String.format("%s, %s",
        nombre,
        provincia.getDireccion()
    );
  }

  @Override
  public String toString() {
    return "Ciudad{nombre=" + nombre + ", provincia=" + provincia + '}';
  }
}
