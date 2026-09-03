package ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Humana;
import jakarta.persistence.CascadeType;
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
public class Representante {

  @Id
  private UUID id = UUID.randomUUID();

  // ManyToOne (no OneToOne): Humana vive en su propio repositorio y podría reutilizarse en otro contexto (ej. Donante).
  @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinColumn(name = "humana_id")
  private Humana humana;

  private boolean activo;

  public Representante(Humana humana, boolean activo) {

    this.humana = humana;
    this.activo = activo;
  }

}
