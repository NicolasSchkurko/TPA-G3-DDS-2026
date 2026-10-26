package ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer;

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

    /** Optimistic locking: una escritura concurrente tira OptimisticLockingFailureException en vez de pisar. */
    @Version
    private Long version;

    @Field(name = "nombre")
    private String nombre;

    @Field(name = "disponible")
    private boolean disponible;

    public Chofer(UUID idChofer, String nombre){
        this.idChofer = idChofer;
        this.nombre = nombre;
        this.disponible = true;
    }

    public Chofer(UUID idChofer, String nombre, boolean disponible){
        this.idChofer = idChofer;
        this.nombre = nombre;
        this.disponible = disponible;
    }

    public void ocupado(){
        this.disponible = false;
    }

    public void disponible(){
        this.disponible = true;
    }
}
