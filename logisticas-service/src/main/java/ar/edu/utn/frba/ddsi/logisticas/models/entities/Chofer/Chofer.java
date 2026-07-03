package ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class Chofer {
    private UUID idChofer;
    private boolean disponible;

    public Chofer(UUID idChofer){
        this.idChofer = idChofer;
        this.disponible = true;
    }

    public Chofer(UUID idChofer, boolean disponible){
        this.idChofer = idChofer;
        this.disponible = disponible;
    }

    public void ocupado(){
        this.disponible = false;
    }

    public void disponible(){
        this.disponible = true;
    }
}
