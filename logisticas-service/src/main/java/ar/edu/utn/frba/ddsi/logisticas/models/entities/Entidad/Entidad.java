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

    /**
     * Control de concurrencia optimista.
     *
     * <p>Sin esto, dos instancias de logistica que leen esta fila y escriben sobre ella lo
     * hacen en silencio: la segunda sobrescribe a la primera con los valores que leyo antes
     * del UPDATE de la otra. Con la version, el UPDATE lleva {@code WHERE version = ?} y si
     * otra instancia escribio en el medio Hibernate tira {@code OptimisticLockingFailureException}
     * en vez de pisar. Es lo que hace seguro el requisito de mas de un servicio de logistica.
     *
     * <p>La columna la crea sola {@code ddl-auto=update}.
     */
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
