package ar.edu.utn.frba.ddsi.logisticas.dto;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class ChoferDTO {
    private UUID idChofer;
    private boolean disponible;
}
