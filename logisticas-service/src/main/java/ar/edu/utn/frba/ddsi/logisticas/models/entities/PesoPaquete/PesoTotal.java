package ar.edu.utn.frba.ddsi.logisticas.models.entities.PesoPaquete;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;

import java.util.List;


public class PesoTotal {
    private final Double kilogramos;
    private final Double volumenM3;

    // Al construirlo, calcula los totales automáticamente de forma segura
    public PesoTotal(List<ItemEntrega> items) {
        this.kilogramos = items.stream()
                .mapToDouble(ItemEntrega::getPesoEstimadoKg)
                .sum();

        this.volumenM3 = items.stream()
                .mapToDouble(ItemEntrega::getVolumenEstimadoM3)
                .sum();
    }

    public Double getKilogramos() {
        return kilogramos;
    }

    public Double getVolumenM3() {
        return volumenM3;
    }

    // Lógica expresiva para comparar contra un camión
    public boolean entraEn(Camion camion) {
        return this.kilogramos <= camion.getCapacidadCarga()
                && this.volumenM3 <= camion.getCapacidadVolumen();
    }
}

