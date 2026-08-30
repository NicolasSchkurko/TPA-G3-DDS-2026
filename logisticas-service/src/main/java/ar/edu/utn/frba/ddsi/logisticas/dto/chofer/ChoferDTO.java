package ar.edu.utn.frba.ddsi.logisticas.dto.chofer;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class ChoferDTO {
    private UUID idChofer;
    private String nombre;
    private boolean disponible;
}