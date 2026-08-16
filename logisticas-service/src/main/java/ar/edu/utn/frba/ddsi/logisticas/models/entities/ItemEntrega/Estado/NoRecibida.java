package ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.Estado;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class NoRecibida extends EstadoEntrega {

    public NoRecibida(ItemEntrega item){
        this.item = item;
    }

    @Override
    public void actualizar() {
        item.cambiarEstado(new Pendiente(item));
        item = null;
    }
}