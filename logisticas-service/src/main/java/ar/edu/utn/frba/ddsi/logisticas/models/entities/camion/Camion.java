package ar.edu.utn.frba.ddsi.logisticas.models.entities.camion;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter

public class Camion {
    private UUID idRuta;
    private String patente;
    private Double capacidadVolumen;
    private Double altura;
    private Double capacidadCarga;
    private Boolean disponible;
}
