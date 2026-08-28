package ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega;
import java.time.LocalDateTime;

public enum EstadoEntrega {
    PENDIENTE,
    EN_CAMINO,
    ENTREGADA,
    NO_RECIBIDA;

    public void cambiarEstado(ItemEntrega item, EstadoEntrega nuevoEstado) {
        item.setEstado(nuevoEstado);
        item.setFechaCambioEstado(LocalDateTime.now());
    }

}