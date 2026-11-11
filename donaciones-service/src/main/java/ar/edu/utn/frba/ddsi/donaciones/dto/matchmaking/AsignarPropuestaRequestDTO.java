package ar.edu.utn.frba.ddsi.donaciones.dto.matchmaking;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class AsignarPropuestaRequestDTO {
    private UUID donacionId;
    private Integer posicionPropuesta;
}