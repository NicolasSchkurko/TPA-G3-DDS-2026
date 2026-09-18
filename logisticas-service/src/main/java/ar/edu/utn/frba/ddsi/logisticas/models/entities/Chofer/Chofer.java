package ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer;

import java.util.UUID;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.*;

@Entity
@Table(name = "chofer")
@Getter
@Setter
@NoArgsConstructor
public class Chofer {
    @Id
    @Column(name = "id_chofer", nullable = false, updatable = false)
    private UUID idChofer;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "disponible", nullable = false)
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