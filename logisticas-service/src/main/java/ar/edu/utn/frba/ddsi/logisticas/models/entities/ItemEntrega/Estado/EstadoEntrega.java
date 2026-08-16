package ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.Estado;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorRutas;
import ar.edu.utn.frba.ddsi.logisticas.services.EventoLogisticaService;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public abstract class EstadoEntrega {
    public ItemEntrega item;
    public EventoLogisticaService eventoService;
    public GestorRutas gestorRutas;

    public EstadoEntrega estadoActual(){
        return this;
    }

    public abstract void actualizar();

    public void marcarNoRecibida(String justificacion){
        item.cambiarEstado(new NoRecibida(item));
        eventoService.publicarEntregaFallida(item, gestorRutas.buscarRuta(item.getIdDonacion()), justificacion);
        item = null;
    }
}
