package ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega;

import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorItemEntrega;

import java.time.LocalDateTime;

public enum EstadoEntrega {
    PENDIENTE,
    EN_CAMINO,
    ENTREGADA,
    NO_RECIBIDA;

    public void cambiarEstado(ItemEntrega item, EstadoEntrega nuevoEstado, GestorItemEntrega gestorItems) {
        item.setEstado(nuevoEstado);
        item.setFechaCambioEstado(LocalDateTime.now());
        gestorItems.guardarItem(item);
    }

}