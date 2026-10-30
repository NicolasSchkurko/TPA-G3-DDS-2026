package ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "pais")
@Getter
@Setter
@NoArgsConstructor // Requerido por JPA
public class Pais {

    @Id
    @Field(name = "id_pais")
    private String idPais;

    @Version
    private Long version;

    @Field(name = "nombre")
    private String nombre;
}
