package ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Direccion;

import java.util.List;

public class PersonaJuridica extends PersonaDonante {
  private Direccion direccion;
  private String razonSocial;
  private String rubro;
  private TipoJuridico tipoJuridico;
  private List<Representante> representantes;

  public PersonaJuridica(
      Direccion direccion,
      String razonSocial,
      String rubro,
      TipoJuridico tipoJuridico,
      List<Representante> representantes
  ) {
    this.direccion = direccion;
    this.razonSocial = razonSocial;
    this.rubro = rubro;
    this.tipoJuridico = tipoJuridico;
    this.representantes = representantes;
  }

}
