package ar.edu.utn.frba.ddsi.donaciones.dto;

import lombok.Getter;

import java.util.UUID;

@Getter
public class AsignarPropuestaRequestDTO {
    private UUID donacionId;
    private Integer posicionPropuesta;
}
