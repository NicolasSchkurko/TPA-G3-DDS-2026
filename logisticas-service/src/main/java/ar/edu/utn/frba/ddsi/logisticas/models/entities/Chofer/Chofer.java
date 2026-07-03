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
    private Camion camionAsignado;

    public Chofer(UUID idChofer, Camion camionAsignado){
        this.idChofer = idChofer;
        this.disponible = true;
        this.camionAsignado = camionAsignado;
    }
}
