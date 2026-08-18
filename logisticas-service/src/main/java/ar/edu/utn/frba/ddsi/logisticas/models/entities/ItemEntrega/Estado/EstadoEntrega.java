package ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.Estado;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorRutas;
import ar.edu.utn.frba.ddsi.logisticas.services.EventoLogisticaService;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public abstract class EstadoEntrega {
    public EventoLogisticaService eventoService;
    public GestorRutas gestorRutas;
    public GestorItemEntrega gestorItems;

    public abstract void actualizar(ItemEntrega item);

    public void marcarNoRecibida(ItemEntrega item, String justificacion){
        eventoService.publicarEntregaFallida(item, gestorRutas.buscarRutaDeIdDonacion(item.getIdDonacion()), justificacion);
    }
}
