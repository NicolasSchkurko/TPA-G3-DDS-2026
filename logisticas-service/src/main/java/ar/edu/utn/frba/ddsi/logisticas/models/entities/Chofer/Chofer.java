package ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "choferes")
public class Chofer {
    @Id
    @Field(name = "id_chofer")
    private UUID idChofer;

    @Version
    private Long version;

    @Field(name = "nombre")
    private String nombre;

    @Field(name = "disponible")
    private boolean disponible;

    @Field(name = "activo")
    private Boolean activo = true;

    @Field(name = "fecha_baja")
    private LocalDateTime fechaBaja;

    public Chofer(UUID idChofer, String nombre){
        this.idChofer = idChofer;
        this.nombre = nombre;
        this.disponible = true;
        this.activo = true;
    }

    public Chofer(UUID idChofer, String nombre, boolean disponible){
        this.idChofer = idChofer;
        this.nombre = nombre;
        this.disponible = disponible;
        this.activo = true;
    }

    public boolean isActivo() {
        return activo == null || activo;
    }

    public void darDeBaja() {
        this.activo = false;
        this.disponible = false;
        this.fechaBaja = LocalDateTime.now();
    }

    public void ocupado(){
        this.disponible = false;
    }

    public void disponible(){
        if (isActivo()) {
            this.disponible = true;
        }
    }
}
