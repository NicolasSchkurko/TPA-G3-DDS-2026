package ar.edu.utn.frba.ddsi.logisticas.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CamionDTO {
    private String nombreChofer;
    private String patente;
    private Double capacidadVolumen; //m2
    private Double altura; //m
    private Double capacidadCarga;
    private Boolean disponible;
}