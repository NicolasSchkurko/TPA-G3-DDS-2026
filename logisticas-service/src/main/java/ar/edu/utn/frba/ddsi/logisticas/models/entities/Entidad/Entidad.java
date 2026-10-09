package ar.edu.utn.frba.ddsi.logisticas.models.entities.Entidad;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Direccion;

import java.util.UUID;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "entidad_destino")
@Getter
@Setter
@NoArgsConstructor
public class Entidad {
  @Id
  @Column(name = "id_entidad_beneficiaria", nullable = false, updatable = false)
  private UUID idEntidadBeneficiaria;

    /** Optimistic locking: una escritura concurrente tira OptimisticLockingFailureException en vez de pisar. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

  @OneToOne
  @JoinColumn(name = "id_direccion_destino", referencedColumnName = "id_direccion", nullable = false)
  private Direccion direccionDestino;

  public Entidad(UUID idEntidadBeneficiaria, Direccion direccionDestino){
    this.idEntidadBeneficiaria = idEntidadBeneficiaria;
    this.direccionDestino = direccionDestino;
  }
}
