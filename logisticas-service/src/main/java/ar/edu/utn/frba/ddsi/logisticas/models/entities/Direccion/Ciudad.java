package ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "ciudad")
@Getter
@Setter
@NoArgsConstructor
public class Ciudad {
    @Id
    @Field(name = "id_ciudad")
    private Long idCiudad;

    @Version
    private Long version;

    @Field(name = "nombre")
    private String nombre;

    private Provincia provincia;

    public Ciudad(String nombre, Provincia provincia){
        this.nombre = nombre;
        this.provincia = provincia;
    }
}
