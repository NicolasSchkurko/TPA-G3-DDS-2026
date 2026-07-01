package ar.edu.utn.frba.ddsi.logisticas.models.entities.PlanificadorDeRutas.ProveedorRutasExterno;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import java.util.List;

/**
 * Interfaz que abstrae la comunicación con la API externa de planificación.
 */
public interface ProveedorRutasExterno {

  /**
   * Envía un lote de ítems y camiones al sistema externo.
   */
  void solicitarPlanificacion(List<ItemEntrega> lote, List<Camion> camionesDisponibles);
}