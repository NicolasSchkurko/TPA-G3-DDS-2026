package ar.edu.utn.frba.ddsi.logisticas.broker.adapters;

import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.PeticionEntregaDTO;
import ar.edu.utn.frba.ddsi.logisticas.services.EntregaService;
import org.springframework.stereotype.Component;

@Component("logisticaPropiaAdapter")
public class LogisticaPropiaAdapter implements ServicioLogisticaAdapter {

    private final EntregaService entregaService;

    public LogisticaPropiaAdapter(EntregaService entregaService) {
        this.entregaService = entregaService;
    }

    @Override
    public String getNombreProveedor() {
        return "PROPIO";
    }

    @Override
    public void procesarEntrega(PeticionEntregaDTO request) {
        // Ejecuta la lógica interna guardando en tu BD local
        entregaService.procesarPeticion(request);
    }
}
