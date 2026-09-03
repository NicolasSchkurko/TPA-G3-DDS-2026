package ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
@Getter
@Setter
@Table(name = "chofer")
public class Chofer {
    @Id
    @Column(name = "id_chofer")
    private UUID idChofer;
    private String nombre;
    private boolean disponible;

    protected Chofer() {
    }

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