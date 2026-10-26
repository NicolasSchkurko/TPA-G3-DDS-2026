package ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "provincia")
@Getter
@Setter
@NoArgsConstructor
public class Provincia {
    @Id
    @Field(name = "id_provincia")
    private String idProvincia;

    @Version
    private Long version;

    @Field(name = "nombre")
    private String nombre;

    private Pais pais;
}

