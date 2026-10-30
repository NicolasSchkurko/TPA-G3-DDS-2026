package ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.UUID;

@Document(collection = "direccion")
@Getter
@Setter
@NoArgsConstructor
public class Direccion {
    @Id
    @Field(name = "id_direccion")
    private UUID idDireccion;

    @Version
    private Long version;

    @Field(name = "calle_1")
    private String calle1;

    @Field(name = "calle_2")
    private String calle2;

    @Field(name = "altura")
    private Integer altura;

    @Field(name = "sin_altura")
    private Boolean sinAltura;

    @Field(name = "piso")
    private Integer piso;

    @Field(name = "departamento")
    private String departamento;

    private Ciudad ciudad;

    public Direccion(String calle1, String calle2, Integer altura, Integer piso, String departamento, String nombreCiudad, String nombreProvincia, String nombrePais){
        this.idDireccion = UUID.randomUUID();
        this.calle1 = calle1;
        this.calle2 = calle2;
        this.altura = altura;
        this.piso = piso;
        this.departamento = departamento;
        Pais pais = new Pais();
        pais.setNombre(nombrePais);
        Provincia provincia = new Provincia();
        provincia.setNombre(nombreProvincia);
        provincia.setPais(pais);
        this.ciudad = new Ciudad(nombreCiudad, provincia);
    }
}
