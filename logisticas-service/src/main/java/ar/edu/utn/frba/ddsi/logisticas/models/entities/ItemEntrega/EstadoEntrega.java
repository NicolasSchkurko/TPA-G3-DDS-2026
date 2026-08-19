package ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega;

import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorItemEntrega;

public enum EstadoEntrega {
    PENDIENTE,
    EN_CAMINO,
    ENTREGADA,
    NO_RECIBIDA;

    public void cambiarEstado(ItemEntrega item, EstadoEntrega nuevoEstado, GestorItemEntrega gestorItems){
        item.cambiarEstado(nuevoEstado);
        gestorItems.guardarItem(item);
    }

}