package ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
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

    // Atributos de estado para la planificación (transitorios)
    private Double pesoOcupado = 0.0;
    private Double volumenOcupado = 0.0;
    private String ciudadDestinoActual = null;

    public boolean puedeCargar(Double pesoKg, Double volumenM3) {
        return pesoKg <= capacidadCarga && volumenM3 <= capacidadVolumen;
    }

    // Lógica expresiva para el planificador
    public boolean puedeCargar(ItemEntrega item) {
        return puedeCargar(this.pesoOcupado + item.getPesoEstimadoKg(),
                           this.volumenOcupado + item.getVolumenEstimadoM3());
    }

    public void cargar(ItemEntrega item, String ciudadDestino) {
        this.pesoOcupado += item.getPesoEstimadoKg();
        this.volumenOcupado += item.getVolumenEstimadoM3();
        this.ciudadDestinoActual = ciudadDestino;
    }

    public void resetearCargaOcupada() {
        this.pesoOcupado = 0.0;
        this.volumenOcupado = 0.0;
        this.ciudadDestinoActual = null;
    }

    public boolean estaVacio() {
        return this.pesoOcupado == 0.0 && this.volumenOcupado == 0.0;
    }

    public void marcarNoDisponible() {
        this.disponible = false;
    }

    public void marcarDisponible() {
        this.disponible = true;
    }
}