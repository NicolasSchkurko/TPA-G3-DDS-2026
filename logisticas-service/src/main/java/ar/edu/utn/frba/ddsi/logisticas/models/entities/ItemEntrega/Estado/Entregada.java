package ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.Estado;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorRutas;
import ar.edu.utn.frba.ddsi.logisticas.services.EventoLogisticaService;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class Entregada extends EstadoEntrega {
    private String fotoComprobante;

    @Override
    public void actualizar(ItemEntrega item) {
        eventoService.publicarEntregaConfirmada(item, gestorRutas.buscarRutaDeIdDonacion((item.getIdDonacion())));
    }
}