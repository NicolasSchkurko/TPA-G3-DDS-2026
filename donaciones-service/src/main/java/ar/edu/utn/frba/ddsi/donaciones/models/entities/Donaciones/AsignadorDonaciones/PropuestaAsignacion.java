package ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.AsignadorDonaciones;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.Necesidades.Necesidad;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class PropuestaAsignacion {
  private final EntidadBeneficiaria entidad;
  private final Necesidad necesidad;

  private String algoritmo;
  private int posicion;
  private double score;

  public PropuestaAsignacion(EntidadBeneficiaria entidad, Necesidad necesidad) {
    this.entidad = entidad;
    this.necesidad = necesidad;
  }

  public PropuestaAsignacion(EntidadBeneficiaria entidad, Necesidad necesidad, String algoritmo, int posicion, double score) {
    this.entidad = entidad;
    this.necesidad = necesidad;
    this.algoritmo = algoritmo;
    this.posicion = posicion;
    this.score = score;
  }

  // La identidad sigue dada estrictamente por la combinación de Entidad y Necesidad
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PropuestaAsignacion that = (PropuestaAsignacion) o;
    return entidad.equals(that.entidad) && necesidad.equals(that.necesidad);
  }

  @Override
  public int hashCode() {
    return Objects.hash(entidad, necesidad);
  }
}