package ar.edu.utn.frba.ddsi.logisticas.broker.adapters;

import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.PeticionEntregaDTO;

public interface ServicioLogisticaAdapter {
    String getNombreProveedor();
    void procesarEntrega(PeticionEntregaDTO request);
}
