package ar.edu.utn.frba.ddsi.logisticas.models.entities.Parada;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Entidad.Entidad;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Direccion;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

@Getter
public class Parada {
    private Direccion direccion;
    private List<ItemEntrega> items = new ArrayList<>();

    // Se crea siempre a partir de un primer item y su dirección
    public Parada(ItemEntrega primerItem) {
        Entidad entidad = primerItem.getEntidadDestino();
        this.direccion = entidad.getDireccionDestino();
        this.items.add(primerItem);
    }

    public void agregarItem(ItemEntrega item) {
        items.add(item);
    }

    public Entidad getEntidadDestino() {
        return items.getFirst().getEntidadDestino();
    }
}