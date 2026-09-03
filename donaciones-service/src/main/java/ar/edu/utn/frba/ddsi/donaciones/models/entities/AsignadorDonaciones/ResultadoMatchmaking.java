package ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
public class ResultadoMatchmaking {

  @Id
  private UUID id = UUID.randomUUID();

  // ManyToOne (no OneToOne): la Donacion vive en su propio repositorio (RepositorioDonaciones).
  // Sin cascade REMOVE: eliminar el resultado de matchmaking no debe borrar la Donacion asociada.
  @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinColumn(name = "donacion_id")
  private Donacion donacion;

  // Las PropuestaAsignacion no tienen repositorio/ciclo de vida propio (se generan al vuelo por
  // los algoritmos de AsignadorDonaciones y sólo cobran sentido dentro de este resultado), así
  // que se mapean como @Embeddable en una tabla de colección propia en lugar de una entidad
  // independiente. @OrderColumn preserva el orden/ranking de las propuestas tal cual se calculó.
  @ElementCollection
  @CollectionTable(name = "resultado_matchmaking_propuestas", joinColumns = @JoinColumn(name = "resultado_id"))
  @OrderColumn(name = "orden")
  private List<PropuestaAsignacion> propuestasOrdenadas;

  // Flag para la Interfaz de Usuario
  private boolean huboCoincidenciaTotal;

  protected ResultadoMatchmaking() {
    // Constructor requerido por JPA/Hibernate.
  }

  public ResultadoMatchmaking(Donacion donacion, List<PropuestaAsignacion> propuestasOrdenadas, boolean huboCoincidenciaTotal) {
    this.donacion = donacion;
    this.propuestasOrdenadas = propuestasOrdenadas;
    this.huboCoincidenciaTotal = huboCoincidenciaTotal;
  }
}
