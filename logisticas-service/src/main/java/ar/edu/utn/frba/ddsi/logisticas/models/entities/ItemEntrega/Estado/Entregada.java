package ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.Estado;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class Entregada extends EstadoEntrega {
    private String fotoComprobante;

    public Entregada(ItemEntrega item){
        this.item = item;
    }

    @Override
    public void actualizar() {
        item.cambiarEstado(new Entregada(item));
        gestorItems.guardarItem(item);
        eventoService.publicarEntregaConfirmada(item, gestorRutas.buscarRutaDeIdDonacion((item.getIdDonacion())));
        item = null;
    }
}