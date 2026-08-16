package ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.Estado;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class Pendiente extends EstadoEntrega {

    public Pendiente(ItemEntrega item){
        this.item = item;
    }

    @Override
    public void actualizar() {
        item.cambiarEstado(new EnCamino(item));
        eventoService.publicarReingresoDeposito(item);
        item = null;
    }
}