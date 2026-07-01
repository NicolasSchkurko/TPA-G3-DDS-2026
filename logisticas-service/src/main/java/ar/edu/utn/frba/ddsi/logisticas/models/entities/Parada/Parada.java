
package ar.edu.utn.frba.ddsi.logisticas.models.entities.Parada;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Entidad.Entidad;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Parada {
    private Entidad entidadBeneficiaria;
    private List<ItemEntrega> items = new ArrayList<>();

    public void agregarItem(ItemEntrega item) {
        items.add(item);
    }

    public Double pesoTotalKg() {
        return items.stream().mapToDouble(ItemEntrega::getPesoEstimadoKg).sum();
    }

    public Double volumenTotalM3() {
        return items.stream().mapToDouble(ItemEntrega::getVolumenEstimadoM3).sum();
    }
}
 
