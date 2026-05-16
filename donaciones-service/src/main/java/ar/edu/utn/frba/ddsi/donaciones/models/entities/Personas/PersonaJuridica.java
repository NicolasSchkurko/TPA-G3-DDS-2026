package ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Direccion;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class PersonaJuridica extends PersonaDonante {

  private Direccion direccion;
  private String razonSocial;
  private String rubro;
  private TipoJuridico tipoJuridico;
  private String cuit;
  private List<Representante> representantes;

  public PersonaJuridica(
      Direccion direccion,
      String razonSocial,
      String rubro,
      TipoJuridico tipoJuridico,
      String cuit,
      List<Representante> representantes
  ) {
    this.direccion = direccion;
    this.razonSocial = razonSocial;
    this.rubro = rubro;
    this.cuit = cuit;
    this.tipoJuridico = tipoJuridico;
    this.representantes = representantes;
  }

}
