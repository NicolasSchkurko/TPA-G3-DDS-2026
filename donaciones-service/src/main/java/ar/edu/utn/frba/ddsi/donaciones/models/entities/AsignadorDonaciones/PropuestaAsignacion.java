package ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

// No tiene repositorio ni ciclo de vida propio: se genera al vuelo dentro de AsignadorDonaciones
// y sólo tiene sentido embebida dentro de la lista propuestasOrdenadas de un ResultadoMatchmaking
// (ver mapeo @ElementCollection allá). Por eso @Embeddable en lugar de @Entity con su propio @Id.
@Embeddable
@Getter
@Setter
public class PropuestaAsignacion {

  // Catálogo ya persistido por su cuenta (EntidadBeneficiaria/Necesidad tienen su propio
  // repositorio): sin cascade, ya vienen resueltas/managed desde la DB antes de armar la
  // propuesta, mismo criterio que Donacion.subcategoria.
  @ManyToOne
  @JoinColumn(name = "entidad_id")
  private EntidadBeneficiaria entidad;

  @ManyToOne
  @JoinColumn(name = "necesidad_id")
  private Necesidad necesidad;

  private String algoritmo;
  private int posicion;
  private double score;

  protected PropuestaAsignacion() {
    // Constructor requerido por JPA/Hibernate.
  }

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
