package ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.Estado;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class EnCamino extends EstadoEntrega {
    public EnCamino(ItemEntrega item){
        this.item = item;
    }

    @Override
    public void actualizar() {
        item.cambiarEstado(new Entregada(item));
        gestorItems.guardarItem(item);
        item = null;
    }
}