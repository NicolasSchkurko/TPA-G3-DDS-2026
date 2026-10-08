package ar.edu.utn.frba.ddsi.logisticas.models.entities.PlanificadorDeRutas.ProveedorRutasExterno;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import java.util.List;

public interface ProveedorRutasExterno {

  void solicitarPlanificacion(List<ItemEntrega> lote, List<Camion> camionesDisponibles);
}