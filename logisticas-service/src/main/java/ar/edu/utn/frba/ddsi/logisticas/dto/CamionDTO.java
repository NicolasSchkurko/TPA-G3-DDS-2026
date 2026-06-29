package ar.edu.utn.frba.ddsi.logisticas.dto;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CamionDTO {
    private UUID idCamion;
    private String patente;
    private Integer capacidadVolumen; //m2
    private Integer altura; //m
    private Integer capacidadCarga; //kg
    private Boolean estaDisponible;
}
