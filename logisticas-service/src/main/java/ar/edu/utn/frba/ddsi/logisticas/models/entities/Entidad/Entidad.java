package ar.edu.utn.frba.ddsi.logisticas.models.entities.Entidad;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Direccion;

import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "entidad_destino")
@Getter
@Setter
@NoArgsConstructor
public class Entidad {
  @Id
  @Field(name = "id_entidad_beneficiaria")
  private UUID idEntidadBeneficiaria;

  @Version
  private Long version;

  private Direccion direccionDestino;

  public Entidad(UUID idEntidadBeneficiaria, Direccion direccionDestino){
    this.idEntidadBeneficiaria = idEntidadBeneficiaria;
    this.direccionDestino = direccionDestino;
  }
}
