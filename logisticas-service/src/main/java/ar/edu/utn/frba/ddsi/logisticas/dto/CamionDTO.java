package ar.edu.utn.frba.ddsi.logisticas.dto;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CamionDTO {
    private UUID idChofer;
    private String patente;
    private Double capacidadVolumen; //m2
    private Double altura; //m
    private Double capacidadCarga;
    private Boolean disponible;
}