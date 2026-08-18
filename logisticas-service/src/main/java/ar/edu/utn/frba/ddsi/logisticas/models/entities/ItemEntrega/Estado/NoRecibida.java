package ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.Estado;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.services.EventoLogisticaService;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class NoRecibida extends EstadoEntrega {

    @Override
    public void actualizar(ItemEntrega item) {
        eventoService.publicarReingresoDeposito(item);
    }
}