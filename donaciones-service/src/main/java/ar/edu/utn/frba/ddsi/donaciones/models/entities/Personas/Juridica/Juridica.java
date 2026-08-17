package ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Persona;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Direccion;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Juridica extends Persona {
  private String razonSocial;
  private String rubro;
  private TipoJuridico tipoJuridico;
  private String cuit;
  private List<Representante> representantes;

  public Juridica(
      String razonSocial,
      String rubro,
      TipoJuridico tipoJuridico,
      String cuit,
      List<Representante> representantes,
      String nombreDeUsuario
  ) {
    super(nombreDeUsuario);
    this.razonSocial = razonSocial;
    this.rubro = rubro;
    this.cuit = cuit;
    this.tipoJuridico = tipoJuridico;
    this.representantes = representantes;
  }

  public String darNombre(){
    return this.getRazonSocial();
  }

  public void agregarRepresentantes(List<Representante> representantes){
      this.representantes.addAll(representantes);
  }

}