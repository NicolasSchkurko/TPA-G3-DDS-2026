package ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion;

import ar.edu.utn.frba.ddsi.logisticas.dto.CamionDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
public class Camion {
    private String patente;
    private Double capacidadVolumen;
    private Double altura;
    private Double capacidadCarga;
    private Boolean disponible;
    private Double pesoOcupado;
    private Double volumenOcupado;
    private String ciudadDestinoActual;

    public Camion(String patente, Double capacidadVolumen, Double altura, Double capacidadCarga, Boolean disponible){
        this.patente = patente;
        this.capacidadVolumen = capacidadVolumen;
        this.altura = altura;
        this.capacidadCarga = capacidadCarga;
        this.disponible = disponible;
        this.pesoOcupado = 0.0;
        this.volumenOcupado = 0.0;
        this.ciudadDestinoActual = null;
    }

    // Atributos de estado para la planificación (transitorios)

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