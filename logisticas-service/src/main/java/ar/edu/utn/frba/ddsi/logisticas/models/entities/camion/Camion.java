package ar.edu.utn.frba.ddsi.logisticas.models.entities.camion;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter

public class Camion {
    String patente;
    Double capacidadVolumen;
    Double altura;
    Double capacidadCarga;
    Boolean disponible;
}
