package ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Persona;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Juridica extends Persona {
  @Column(length = 500)
  private String razonSocial;
  private String rubro;

  @Enumerated(EnumType.STRING)
  private TipoJuridico tipoJuridico;

  private String cuit;

  // Unidireccional, mismo patrón que EntidadBeneficiaria->Necesidad: Representante no tiene referencia de vuelta.
  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "juridica_id")
  private List<Representante> representantes = new ArrayList<>();

  public Juridica(
      String razonSocial,
      String rubro,
      TipoJuridico tipoJuridico,
      String cuit,
      List<Representante> representantes
  ) {
    super();
    if (razonSocial == null || razonSocial.isBlank()) {
      throw new IllegalArgumentException("La persona jurídica requiere razón social");
    }
    this.razonSocial = razonSocial.trim();
    this.rubro = rubro;
    this.cuit = cuit;
    this.tipoJuridico = tipoJuridico;
    this.representantes = representantes;
  }

  @Override
  public String getNombreDeUsuario() {
    if (this.razonSocial == null || this.razonSocial.isBlank()) {
      throw new IllegalStateException("La persona jurídica no tiene razón social");
    }
    return this.razonSocial.trim();
  }

  public void agregarRepresentantes(List<Representante> representantes) {
    this.representantes.addAll(representantes);
  }
}
