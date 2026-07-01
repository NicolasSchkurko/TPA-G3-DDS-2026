package ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class Camion {
    private UUID idCamion;
    private String patente;
    private Double capacidadVolumen;
    private Double altura;
    private Double capacidadCarga;
    private Boolean disponible;

    public boolean puedeCargar(Double pesoKg, Double volumenM3) {
        return pesoKg <= capacidadCarga && volumenM3 <= capacidadVolumen;
    }

    public void marcarNoDisponible() {
        this.disponible = false;
    }

    public void marcarDisponible() {
        this.disponible = true;
    }
}