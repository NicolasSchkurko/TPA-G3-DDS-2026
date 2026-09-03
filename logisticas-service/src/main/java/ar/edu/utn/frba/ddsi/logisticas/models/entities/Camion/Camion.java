package ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
public class Camion {
    @Column(name = "id_chofer")
    private UUID idChofer;
    @Id
    private String patente;
    private Double capacidadVolumen;
    private Double altura;
    private Double capacidadCarga;
    private Boolean disponible;
    private Double pesoOcupado;
    private Double volumenOcupado;
    private String ciudadDestinoActual;

    public Camion(UUID idChofer, String patente, Double capacidadVolumen, Double altura, Double capacidadCarga, Boolean disponible){
        this.idChofer = idChofer;
        this.patente = patente;
        this.capacidadVolumen = capacidadVolumen;
        this.altura = altura;
        this.capacidadCarga = capacidadCarga;
        this.disponible = disponible;
        this.pesoOcupado = 0.0;
        this.volumenOcupado = 0.0;
        this.ciudadDestinoActual = null;
    }

    public Camion(String patente, Double capacidadVolumen, Double altura, Double capacidadCarga, Boolean disponible){
        this.idChofer = null;
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

    public void ocupado() {
        this.disponible = false;
    }

    public void disponible() {
        this.disponible = true;
    }

    public void eliminarChofer() {
        this.idChofer = null;
    }
}